package com.abap.assistant.ui;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.abap.assistant.core.AbapAnalysisResult;
import com.abap.assistant.core.AbapContextClassifier;
import com.abap.assistant.core.AbapDependencyAnalyzer;
import com.abap.assistant.core.AssistantMode;
import com.abap.assistant.core.AssistantPromptBuilder;
import com.abap.assistant.core.AssistantRequest;
import com.abap.assistant.core.AssistantResponse;
import com.abap.assistant.core.AssistantService;
import com.abap.assistant.core.OpenAiResponsesClient;
import com.abap.assistant.core.OpenAiSettings;
import com.abap.assistant.core.SensitiveDataRedactor;
import com.abap.assistant.core.SuggestedChangeReview;
import com.abap.assistant.core.SuggestedChangeReviewBuilder;
import com.abap.assistant.eclipse.EclipseDotEnvLocator;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.texteditor.ITextEditor;

public final class ChatView extends ViewPart {
    public static final String ID = "com.abap.assistant.ui.ChatView";
    private static final int MAX_CONTEXT_CHARS = 60000;
    private static final int MAX_RELATED_FILES = 10;
    private static final int MAX_RELATED_FILE_CHARS = 18000;
    private static final int MAX_HISTORY_TURNS = 6;
    private static final int MAX_HISTORY_CHARS = 18000;
    private static final Pattern FENCED_CODE_BLOCK = Pattern.compile("(?is)```\\s*(?:abap)?\\s*\\R?(.*?)```");

    private Color transcriptBackground;
    private Color transcriptForeground;
    private Color mutedForeground;
    private Color userForeground;
    private Color codeBackground;
    private Color codeForeground;
    private Color errorForeground;
    private Combo modeCombo;
    private Text questionText;
    private Label statusLabel;
    private StyledText transcriptText;
    private Button askButton;
    private Button clearButton;
    private Button copyResponseButton;
    private Button copyCodeButton;
    private final AbapDependencyAnalyzer dependencyAnalyzer = new AbapDependencyAnalyzer();
    private final SuggestedChangeReviewBuilder reviewBuilder = new SuggestedChangeReviewBuilder();
    private final List<ConversationTurn> conversationHistory = new ArrayList<>();
    private String lastResponseText = "";
    private String lastSuggestionText = "";
    private int pendingStart = -1;
    private int pendingLength = 0;

    @Override
    public void createPartControl(Composite parent) {
        initializeColors(parent.getDisplay());
        parent.setLayout(new GridLayout(1, false));

        Composite header = new Composite(parent, SWT.NONE);
        header.setLayoutData(fillHorizontal());
        header.setLayout(new GridLayout(7, false));

        Label title = new Label(header, SWT.NONE);
        title.setText("ABAP Chat Assistant");
        title.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));

        Label modeLabel = new Label(header, SWT.NONE);
        modeLabel.setText("Mode");
        modeLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));

        modeCombo = new Combo(header, SWT.READ_ONLY);
        for (AssistantMode mode : AssistantMode.values()) {
            modeCombo.add(mode.label());
        }
        modeCombo.select(0);
        modeCombo.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

        clearButton = new Button(header, SWT.PUSH);
        clearButton.setText("Clear chat");
        clearButton.setToolTipText("Clear the visible conversation history");
        clearButton.addListener(SWT.Selection, event -> clearChat());

        copyResponseButton = new Button(header, SWT.PUSH);
        copyResponseButton.setText("Copy response");
        copyResponseButton.setToolTipText("Copy the last assistant response");
        copyResponseButton.setEnabled(false);
        copyResponseButton.addListener(SWT.Selection, event -> copyText(lastResponseText, "Response copied"));

        copyCodeButton = new Button(header, SWT.PUSH);
        copyCodeButton.setText("Copy ABAP code");
        copyCodeButton.setToolTipText("Copy the last ABAP code block with the manual-review header");
        copyCodeButton.setEnabled(false);
        copyCodeButton.addListener(SWT.Selection, event -> copyText(lastSuggestionText, "ABAP code copied for manual review"));

        statusLabel = new Label(header, SWT.NONE);
        statusLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));

        transcriptText = new StyledText(parent, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.WRAP | SWT.READ_ONLY);
        transcriptText.setEditable(false);
        transcriptText.setBackground(transcriptBackground);
        transcriptText.setForeground(transcriptForeground);
        transcriptText.setLayoutData(fillBoth(520));

        Composite composer = new Composite(parent, SWT.NONE);
        composer.setLayoutData(fillHorizontal());
        composer.setLayout(new GridLayout(2, false));

        questionText = new Text(composer, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
        questionText.setMessage("Ask about the open ABAP editors...");
        questionText.setBackground(transcriptBackground);
        questionText.setForeground(transcriptForeground);
        questionText.setLayoutData(fillBoth(82));
        questionText.addListener(SWT.KeyDown, event -> {
            boolean ctrlEnter = (event.stateMask & SWT.CTRL) != 0
                && (event.keyCode == SWT.CR || event.keyCode == SWT.KEYPAD_CR);
            if (ctrlEnter) {
                event.doit = false;
                askAssistant();
            }
        });

        askButton = new Button(composer, SWT.PUSH);
        askButton.setText("Ask");
        askButton.setToolTipText("Send the question with the current open-editor context");
        askButton.setLayoutData(new GridData(SWT.RIGHT, SWT.BOTTOM, false, false));
        askButton.addListener(SWT.Selection, event -> askAssistant());

        addSystemMessage("Ready. Open ABAP/text editors are read automatically. Suggested code is copy-only; the plug-in does not write to SAP.");
        setStatus("Ready - open editors are read automatically");
    }

    @Override
    public void setFocus() {
        questionText.setFocus();
    }

    @Override
    public void dispose() {
        disposeColor(errorForeground);
        disposeColor(codeForeground);
        disposeColor(codeBackground);
        disposeColor(userForeground);
        disposeColor(mutedForeground);
        disposeColor(transcriptForeground);
        disposeColor(transcriptBackground);
        super.dispose();
    }

    private void initializeColors(Display display) {
        transcriptBackground = new Color(display, 39, 43, 45);
        transcriptForeground = new Color(display, 238, 242, 244);
        mutedForeground = new Color(display, 185, 196, 201);
        userForeground = new Color(display, 224, 242, 255);
        codeBackground = new Color(display, 27, 31, 34);
        codeForeground = new Color(display, 255, 255, 255);
        errorForeground = new Color(display, 255, 190, 190);
    }

    private static void disposeColor(Color color) {
        if (color != null && !color.isDisposed()) {
            color.dispose();
        }
    }

    private void askAssistant() {
        String question = questionText.getText();
        ContextSnapshot snapshot = buildContextSnapshot();
        String context = snapshot.promptContextWithHistory(formatConversationHistory());

        if ((question == null || question.isBlank()) && (context == null || context.isBlank())) {
            setStatus("Enter a question or open an ABAP/text editor first");
            return;
        }

        AssistantMode mode = AssistantMode.values()[Math.max(0, modeCombo.getSelectionIndex())];
        AssistantRequest request = new AssistantRequest(mode, question, context);
        questionText.setText("");
        lastResponseText = "";
        lastSuggestionText = "";
        addUserMessage(blankFallback(question), snapshot.contextLine(conversationHistory.size()));
        addPendingMessage();
        setBusy(true);
        setStatus("Calling OpenAI - " + snapshot.openEditorCount() + " editor(s), "
            + snapshot.relatedContextCount() + " related source(s)");

        Job job = new Job("ABAP Chat Assistant request") {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                try {
                    AssistantService service = new AssistantService(
                        new AssistantPromptBuilder(new SensitiveDataRedactor(), new AbapContextClassifier()),
                        new OpenAiResponsesClient(OpenAiSettings.fromEnvironment(EclipseDotEnvLocator.candidateDotEnvFiles())));
                    AssistantResponse response = service.answer(request);
                    updateUi(() -> {
                        removePendingMessage();
                        SuggestedChangeReview review = reviewBuilder.build(snapshot.originalExcerpt(), response.text());
                        addAssistantMessage(response.text(), review, response.privacyScope().toString());
                        addConversationTurn(question, response.text());
                        setStatus("Done - " + response.privacyScope());
                        setBusy(false);
                    });
                    return Status.OK_STATUS;
                } catch (Exception exception) {
                    updateUi(() -> {
                        removePendingMessage();
                        addErrorMessage(exception.getMessage());
                        setStatus("Request failed");
                        setBusy(false);
                    });
                    return new Status(IStatus.ERROR, "com.abap.assistant", exception.getMessage(), exception);
                }
            }
        };
        job.setUser(true);
        job.schedule();
    }

    private void updateUi(Runnable runnable) {
        Display display = getSite().getShell().getDisplay();
        if (!display.isDisposed()) {
            display.asyncExec(runnable);
        }
    }

    private void setBusy(boolean busy) {
        askButton.setEnabled(!busy);
        clearButton.setEnabled(!busy);
        modeCombo.setEnabled(!busy);
        questionText.setEnabled(!busy);
        copyResponseButton.setEnabled(!busy && !lastResponseText.isBlank());
        copyCodeButton.setEnabled(!busy && !lastSuggestionText.isBlank());
    }

    private void setStatus(String value) {
        statusLabel.setText(value == null ? "" : value);
    }

    private void clearChat() {
        transcriptText.setText("");
        pendingStart = -1;
        pendingLength = 0;
        lastResponseText = "";
        lastSuggestionText = "";
        conversationHistory.clear();
        addSystemMessage("Chat cleared. Open ABAP/text editors will be read again on the next ask.");
        setStatus("Ready - chat cleared");
        setBusy(false);
    }

    private void addUserMessage(String question, String contextLine) {
        appendRole("You", userForeground, SWT.RIGHT);
        appendMuted(contextLine, SWT.RIGHT);
        appendPlain(question, userForeground, SWT.RIGHT);
        appendBlankLine();
        refreshTranscript();
    }

    private void addAssistantMessage(String answer, SuggestedChangeReview review, String privacyScope) {
        lastResponseText = answer == null ? "" : answer;
        lastSuggestionText = review.hasSuggestion() ? review.copyText() : "";
        appendRole("Assistant");
        appendMuted("Privacy scope: " + privacyScope + " | Copy-only response. No SAP write or activation is performed.");
        appendAssistantContent(lastResponseText);
        if (review.hasSuggestion()) {
            appendMuted("Use Copy ABAP code to copy the last code block with the manual-review header.");
        }
        appendBlankLine();
        setBusy(false);
        refreshTranscript();
    }

    private void addSystemMessage(String message) {
        appendRole("System");
        appendPlain(message == null ? "" : message);
        appendBlankLine();
        refreshTranscript();
    }

    private void addErrorMessage(String message) {
        appendRole("Error", errorForeground, SWT.LEFT);
        appendPlain(message == null || message.isBlank() ? "Request failed." : message, errorForeground, SWT.LEFT);
        appendBlankLine();
        refreshTranscript();
    }

    private void addPendingMessage() {
        pendingStart = transcriptText.getCharCount();
        addSystemMessage("Thinking...");
        pendingLength = transcriptText.getCharCount() - pendingStart;
    }

    private void removePendingMessage() {
        if (pendingStart >= 0 && pendingLength > 0 && pendingStart + pendingLength <= transcriptText.getCharCount()) {
            transcriptText.replaceTextRange(pendingStart, pendingLength, "");
        }
        pendingStart = -1;
        pendingLength = 0;
        refreshTranscript();
    }

    private void refreshTranscript() {
        if (transcriptText == null || transcriptText.isDisposed()) {
            return;
        }
        transcriptText.setTopIndex(Math.max(0, transcriptText.getLineCount() - 1));
    }

    private void copyText(String value, String successStatus) {
        if (value == null || value.isBlank()) {
            setStatus("Nothing to copy");
            return;
        }
        Clipboard clipboard = new Clipboard(getSite().getShell().getDisplay());
        try {
            clipboard.setContents(new Object[] { value }, new Transfer[] { TextTransfer.getInstance() });
            setStatus(successStatus);
        } finally {
            clipboard.dispose();
        }
    }

    private void appendAssistantContent(String value) {
        String text = value == null ? "" : value;
        Matcher matcher = FENCED_CODE_BLOCK.matcher(text);
        int position = 0;
        boolean foundCode = false;
        while (matcher.find()) {
            appendPlain(text.substring(position, matcher.start()));
            appendCodeBlock(matcher.group(1));
            position = matcher.end();
            foundCode = true;
        }
        appendPlain(text.substring(position));
        if (!foundCode && text.isBlank()) {
            appendPlain("(empty response)");
        }
    }

    private void appendRole(String role) {
        appendRole(role, transcriptForeground, SWT.LEFT);
    }

    private void appendRole(String role, Color foreground, int alignment) {
        appendStyled(role + System.lineSeparator(), SWT.BOLD, foreground, null, alignment);
    }

    private void appendMuted(String value) {
        appendMuted(value, SWT.LEFT);
    }

    private void appendMuted(String value, int alignment) {
        if (value == null || value.isBlank()) {
            return;
        }
        appendStyled(value.strip() + System.lineSeparator(), SWT.NORMAL, mutedForeground, null, alignment);
    }

    private void appendPlain(String value) {
        appendPlain(value, transcriptForeground, SWT.LEFT);
    }

    private void appendPlain(String value, Color foreground, int alignment) {
        if (value == null || value.isBlank()) {
            return;
        }
        appendStyled(value.strip() + System.lineSeparator(), SWT.NORMAL, foreground, null, alignment);
    }

    private void appendCodeBlock(String code) {
        String normalizedCode = code == null ? "" : code.strip();
        appendStyled("ABAP code" + System.lineSeparator(), SWT.BOLD, codeForeground, null, SWT.LEFT);
        String block = "----------------------------------------" + System.lineSeparator()
            + normalizedCode + System.lineSeparator()
            + "----------------------------------------" + System.lineSeparator();
        appendStyled(block, SWT.NORMAL, codeForeground, codeBackground, SWT.LEFT);
    }

    private void appendBlankLine() {
        appendStyled(System.lineSeparator(), SWT.NORMAL, transcriptForeground, null, SWT.LEFT);
    }

    private void appendStyled(String value, int fontStyle, Color foreground, Color background, int alignment) {
        if (value == null || value.isEmpty()) {
            return;
        }
        int start = transcriptText.getCharCount();
        transcriptText.append(value);
        int end = transcriptText.getCharCount();
        StyleRange range = new StyleRange();
        range.start = start;
        range.length = value.length();
        range.fontStyle = fontStyle;
        range.foreground = foreground == null ? transcriptForeground : foreground;
        range.background = background;
        transcriptText.setStyleRange(range);
        int startLine = transcriptText.getLineAtOffset(start);
        int endLine = transcriptText.getLineAtOffset(Math.max(start, end - 1));
        transcriptText.setLineAlignment(startLine, endLine - startLine + 1, alignment);
    }

    private EditorContext readEditor(IEditorPart editor) {
        if (editor == null) {
            return null;
        }

        ITextEditor textEditor = asTextEditor(editor);
        if (textEditor == null || textEditor.getDocumentProvider() == null) {
            return null;
        }

        IDocument document = textEditor.getDocumentProvider().getDocument(textEditor.getEditorInput());
        if (document == null || document.get().isBlank()) {
            return null;
        }
        IFile file = editorFile(textEditor);
        return new EditorContext(editor.getTitle(), document.get(), file);
    }

    private static IFile editorFile(ITextEditor textEditor) {
        Object fileAdapter = textEditor.getEditorInput().getAdapter(IFile.class);
        return fileAdapter instanceof IFile ? (IFile) fileAdapter : null;
    }

    private ITextEditor asTextEditor(IEditorPart editor) {
        if (editor instanceof ITextEditor) {
            return (ITextEditor) editor;
        }

        Object adapter = editor.getAdapter(ITextEditor.class);
        if (adapter instanceof ITextEditor) {
            return (ITextEditor) adapter;
        }
        return null;
    }

    private ContextSnapshot buildContextSnapshot() {
        List<EditorContext> openEditors = readOpenEditors();
        AbapAnalysisResult openEditorAnalysis = dependencyAnalyzer.analyze(combinedText(openEditors));
        List<EditorContext> relatedContexts = resolveRelatedWorkspaceSources(openEditors, openEditorAnalysis.referenceNames());
        List<EditorContext> allResolvedContexts = new ArrayList<>(openEditors);
        allResolvedContexts.addAll(relatedContexts);
        AbapAnalysisResult analysis = dependencyAnalyzer.analyze(combinedText(allResolvedContexts));
        List<String> unresolvedReferences = unresolvedReferences(analysis.referenceNames(), allResolvedContexts);
        return new ContextSnapshot(openEditors, relatedContexts, analysis, unresolvedReferences);
    }

    private List<EditorContext> readOpenEditors() {
        List<EditorContext> contexts = new ArrayList<>();
        IEditorPart activeEditor = getSite().getPage().getActiveEditor();
        EditorContext activeContext = readEditor(activeEditor);
        if (activeContext != null) {
            contexts.add(activeContext);
        }

        for (IEditorReference reference : getSite().getPage().getEditorReferences()) {
            IEditorPart editor = reference.getEditor(false);
            if (editor == null) {
                editor = reference.getEditor(true);
            }
            if (editor == activeEditor) {
                continue;
            }

            EditorContext context = readEditor(editor);
            if (context != null && !containsTitle(contexts, context.title())) {
                contexts.add(context);
            }
        }
        return contexts;
    }

    private List<EditorContext> resolveRelatedWorkspaceSources(List<EditorContext> openEditors, List<String> referenceNames) {
        if (openEditors.isEmpty() || referenceNames.isEmpty()) {
            return List.of();
        }

        Set<IProject> projects = new LinkedHashSet<>();
        Set<String> openFingerprints = new HashSet<>();
        for (EditorContext context : openEditors) {
            if (context.file() != null && context.file().getProject() != null) {
                projects.add(context.file().getProject());
            }
            openFingerprints.add(context.fingerprint());
        }

        if (projects.isEmpty()) {
            return List.of();
        }

        List<EditorContext> relatedContexts = new ArrayList<>();
        Set<String> relatedPaths = new HashSet<>();
        for (IProject project : projects) {
            if (relatedContexts.size() >= MAX_RELATED_FILES) {
                break;
            }
            collectRelatedSources(project, referenceNames, openFingerprints, relatedPaths, relatedContexts);
        }
        return relatedContexts;
    }

    private void collectRelatedSources(
        IProject project,
        List<String> referenceNames,
        Set<String> openFingerprints,
        Set<String> relatedPaths,
        List<EditorContext> relatedContexts) {

        try {
            project.accept(resource -> {
                if (relatedContexts.size() >= MAX_RELATED_FILES) {
                    return false;
                }
                if (resource instanceof IContainer && shouldSkipContainer(resource)) {
                    return false;
                }
                if (!(resource instanceof IFile)) {
                    return true;
                }

                IFile file = (IFile) resource;
                if (!isReadableTextCandidate(file) || !matchesAnyReference(file, referenceNames)) {
                    return true;
                }

                String path = file.getFullPath().toString();
                if (relatedPaths.contains(path)) {
                    return true;
                }

                String text = readFileText(file, MAX_RELATED_FILE_CHARS);
                if (text.isBlank()) {
                    return true;
                }

                EditorContext context = new EditorContext("Related source: " + file.getProjectRelativePath(), text, file);
                if (!openFingerprints.contains(context.fingerprint())) {
                    relatedContexts.add(context);
                    relatedPaths.add(path);
                }
                return true;
            });
        } catch (CoreException exception) {
            setStatus("Some related workspace sources could not be inspected: " + exception.getMessage());
        }
    }

    private static boolean shouldSkipContainer(IResource resource) {
        String name = resource.getName();
        return ".git".equals(name)
            || ".metadata".equals(name)
            || "bin".equals(name)
            || "build".equals(name)
            || "target".equals(name)
            || "node_modules".equals(name)
            || ".settings".equals(name);
    }

    private static boolean isReadableTextCandidate(IFile file) {
        String name = file.getName().toLowerCase(Locale.ROOT);
        if (name.endsWith(".class")
            || name.endsWith(".jar")
            || name.endsWith(".png")
            || name.endsWith(".jpg")
            || name.endsWith(".jpeg")
            || name.endsWith(".gif")
            || name.endsWith(".zip")
            || name.endsWith(".pdf")
            || name.endsWith(".dll")
            || name.endsWith(".exe")) {
            return false;
        }
        try {
            if (!file.exists()) {
                return false;
            }
            if (file.getLocation() == null) {
                return true;
            }
            return file.getLocation().toFile().length() <= MAX_RELATED_FILE_CHARS * 4L;
        } catch (RuntimeException exception) {
            return false;
        }
    }

    private static boolean matchesAnyReference(IFile file, List<String> referenceNames) {
        String path = normalizeReference(file.getProjectRelativePath().toString());
        String name = normalizeReference(file.getName());
        for (String referenceName : referenceNames) {
            String reference = normalizeReference(referenceName);
            if (!reference.isBlank() && (name.contains(reference) || path.contains(reference))) {
                return true;
            }
        }
        return false;
    }

    private static String readFileText(IFile file, int maxChars) {
        try (InputStream stream = file.getContents(true)) {
            byte[] bytes = stream.readAllBytes();
            String text = new String(bytes, StandardCharsets.UTF_8);
            if (text.length() <= maxChars) {
                return text;
            }
            return text.substring(0, maxChars)
                + System.lineSeparator()
                + "[Related source truncated locally.]";
        } catch (CoreException | IOException exception) {
            return "";
        }
    }

    private static List<String> unresolvedReferences(List<String> detectedReferences, List<EditorContext> relatedContexts) {
        if (detectedReferences.isEmpty()) {
            return List.of();
        }

        List<String> unresolved = new ArrayList<>();
        for (String reference : detectedReferences) {
            boolean resolved = false;
            String normalizedReference = normalizeReference(reference);
            for (EditorContext context : relatedContexts) {
                String normalizedTitle = normalizeReference(context.title());
                if (normalizedTitle.contains(normalizedReference)) {
                    resolved = true;
                    break;
                }
            }
            if (!resolved) {
                unresolved.add(reference);
            }
        }
        return unresolved;
    }

    private static boolean containsTitle(List<EditorContext> contexts, String title) {
        for (EditorContext context : contexts) {
            if (context.title().equals(title)) {
                return true;
            }
        }
        return false;
    }

    private static String formatEditorContexts(List<EditorContext> contexts) {
        if (contexts == null || contexts.isEmpty()) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        for (EditorContext context : contexts) {
            if (builder.length() > 0) {
                builder.append(System.lineSeparator()).append(System.lineSeparator());
            }
            builder.append(context.format());
        }
        return builder.toString();
    }

    private static String combinedText(List<EditorContext> contexts) {
        if (contexts == null || contexts.isEmpty()) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        for (EditorContext context : contexts) {
            if (builder.length() > 0) {
                builder.append(System.lineSeparator()).append(System.lineSeparator());
            }
            builder.append(context.title()).append(System.lineSeparator());
            builder.append(context.text());
        }
        return builder.toString();
    }

    private String formatConversationHistory() {
        if (conversationHistory.isEmpty()) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        int start = Math.max(0, conversationHistory.size() - MAX_HISTORY_TURNS);
        for (int index = start; index < conversationHistory.size(); index++) {
            ConversationTurn turn = conversationHistory.get(index);
            builder.append("User: ").append(turn.question()).append(System.lineSeparator());
            builder.append("Assistant: ").append(turn.answer()).append(System.lineSeparator()).append(System.lineSeparator());
        }
        String history = builder.toString().strip();
        if (history.length() <= MAX_HISTORY_CHARS) {
            return history;
        }
        return history.substring(history.length() - MAX_HISTORY_CHARS)
            + System.lineSeparator()
            + "[Older conversation history truncated locally.]";
    }

    private void addConversationTurn(String question, String answer) {
        conversationHistory.add(new ConversationTurn(question, answer));
        while (conversationHistory.size() > MAX_HISTORY_TURNS) {
            conversationHistory.remove(0);
        }
    }

    private static String trimContext(String context) {
        if (context == null) {
            return "";
        }
        if (context.length() <= MAX_CONTEXT_CHARS) {
            return context;
        }
        return context.substring(0, MAX_CONTEXT_CHARS)
            + System.lineSeparator()
            + "[Context truncated locally to keep the request bounded.]";
    }

    private static String normalizeReference(String value) {
        if (value == null) {
            return "";
        }
        return value.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9_/]", "");
    }

    private static String blankFallback(String value) {
        return value == null || value.isBlank() ? "(no explicit question)" : value.strip();
    }

    private static GridData fillHorizontal() {
        return new GridData(SWT.FILL, SWT.CENTER, true, false);
    }

    private static GridData fillBoth(int heightHint) {
        GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
        data.heightHint = heightHint;
        return data;
    }

    private static final class EditorContext {
        private final String title;
        private final String text;
        private final IFile file;

        private EditorContext(String title, String text) {
            this(title, text, null);
        }

        private EditorContext(String title, String text, IFile file) {
            this.title = title == null || title.isBlank() ? "Untitled editor" : title;
            this.text = text == null ? "" : text;
            this.file = file;
        }

        private String title() {
            return title;
        }

        private IFile file() {
            return file;
        }

        private String text() {
            return text;
        }

        private String fingerprint() {
            String path = file == null ? "" : file.getFullPath().toString();
            return title + ":" + path + ":" + Integer.toHexString(text.hashCode());
        }

        private String format() {
            return "Editor: " + title + System.lineSeparator()
                + "```abap" + System.lineSeparator()
                + text
                + System.lineSeparator()
                + "```";
        }
    }

    private static final class ConversationTurn {
        private final String question;
        private final String answer;

        private ConversationTurn(String question, String answer) {
            this.question = question == null || question.isBlank() ? "(no explicit question)" : question.strip();
            this.answer = answer == null ? "" : answer.strip();
        }

        private String question() {
            return question;
        }

        private String answer() {
            return answer;
        }
    }

    private static final class ContextSnapshot {
        private final List<EditorContext> openEditors;
        private final List<EditorContext> relatedContexts;
        private final AbapAnalysisResult analysis;
        private final List<String> unresolvedReferences;

        private ContextSnapshot(
            List<EditorContext> openEditors,
            List<EditorContext> relatedContexts,
            AbapAnalysisResult analysis,
            List<String> unresolvedReferences) {
            this.openEditors = openEditors;
            this.relatedContexts = relatedContexts;
            this.analysis = analysis;
            this.unresolvedReferences = unresolvedReferences;
        }

        private int openEditorCount() {
            return openEditors.size();
        }

        private int relatedContextCount() {
            return relatedContexts.size();
        }

        private String contextLine(int historyTurns) {
            return "Using " + openEditors.size() + " editor(s)"
                + " | " + relatedContexts.size() + " related source(s)"
                + " | " + analysis.references().size() + " reference(s)"
                + " | " + unresolvedReferences.size() + " unresolved"
                + " | " + analysis.customReferences().size() + " custom/Z"
                + " | " + analysis.riskSignals().size() + " risk(s)"
                + " | history " + historyTurns;
        }

        private String promptContextWithHistory(String history) {
            StringBuilder builder = new StringBuilder();
            appendSection(builder, "Open editor context", formatEditorContexts(openEditors));
            appendSection(builder, "Related workspace sources", formatEditorContexts(relatedContexts));
            appendSection(builder, "Local ABAP dependency and risk summary", analysis.summaryText(unresolvedReferences));
            appendSection(builder, "Recent conversation history", history);
            return trimContext(builder.toString());
        }

        private String originalExcerpt() {
            return combinedText(openEditors);
        }

        private static void appendSection(StringBuilder builder, String title, String content) {
            if (content == null || content.isBlank()) {
                return;
            }
            if (builder.length() > 0) {
                builder.append(System.lineSeparator()).append(System.lineSeparator());
            }
            builder.append("## ").append(title).append(System.lineSeparator());
            builder.append(content.strip());
        }
    }
}
