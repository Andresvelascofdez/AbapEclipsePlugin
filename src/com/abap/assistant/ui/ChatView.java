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
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
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
    private static final int RESPONSE_MAX_HEIGHT = 460;
    private static final int REVIEW_MAX_HEIGHT = 280;

    private Combo modeCombo;
    private Text questionText;
    private Label statusLabel;
    private ScrolledComposite transcriptScroll;
    private Composite transcript;
    private Composite pendingMessage;
    private Button askButton;
    private Button clearButton;
    private final AbapDependencyAnalyzer dependencyAnalyzer = new AbapDependencyAnalyzer();
    private final SuggestedChangeReviewBuilder reviewBuilder = new SuggestedChangeReviewBuilder();
    private final List<ConversationTurn> conversationHistory = new ArrayList<>();

    @Override
    public void createPartControl(Composite parent) {
        parent.setLayout(new GridLayout(1, false));

        Composite header = new Composite(parent, SWT.NONE);
        header.setLayoutData(fillHorizontal());
        header.setLayout(new GridLayout(5, false));

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

        statusLabel = new Label(header, SWT.NONE);
        statusLabel.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, false, false));

        transcriptScroll = new ScrolledComposite(parent, SWT.BORDER | SWT.V_SCROLL);
        transcriptScroll.setExpandHorizontal(true);
        transcriptScroll.setExpandVertical(true);
        transcriptScroll.setLayoutData(fillBoth(460));

        transcript = new Composite(transcriptScroll, SWT.NONE);
        GridLayout transcriptLayout = new GridLayout(1, false);
        transcriptLayout.marginWidth = 10;
        transcriptLayout.marginHeight = 10;
        transcriptLayout.verticalSpacing = 10;
        transcript.setLayout(transcriptLayout);
        transcriptScroll.setContent(transcript);

        Composite composer = new Composite(parent, SWT.NONE);
        composer.setLayoutData(fillHorizontal());
        composer.setLayout(new GridLayout(2, false));

        questionText = new Text(composer, SWT.BORDER | SWT.MULTI | SWT.WRAP | SWT.V_SCROLL);
        questionText.setMessage("Ask about the open ABAP editors...");
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
        addUserMessage(blankFallback(question), snapshot.contextLine(conversationHistory.size()));
        pendingMessage = addSystemMessage("Thinking...");
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
    }

    private void setStatus(String value) {
        statusLabel.setText(value == null ? "" : value);
    }

    private void clearChat() {
        for (Control child : transcript.getChildren()) {
            child.dispose();
        }
        pendingMessage = null;
        conversationHistory.clear();
        addSystemMessage("Chat cleared. Open ABAP/text editors will be read again on the next ask.");
        setStatus("Ready - chat cleared");
        refreshTranscript();
    }

    private void addUserMessage(String question, String contextLine) {
        Composite card = createMessageCard("You");
        Label context = new Label(card, SWT.WRAP);
        context.setText(contextLine);
        context.setLayoutData(fillHorizontal());
        Text body = new Text(card, SWT.MULTI | SWT.WRAP | SWT.READ_ONLY);
        body.setText(question);
        body.setLayoutData(textBlockData(question, 52, 180));
        refreshTranscript();
    }

    private void addAssistantMessage(String answer, SuggestedChangeReview review, String privacyScope) {
        Composite card = createMessageCard("Assistant");

        Label metadata = new Label(card, SWT.WRAP);
        metadata.setText("Privacy scope: " + privacyScope + " | Copy-only response. No SAP write or activation is performed.");
        metadata.setLayoutData(fillHorizontal());

        Text body = new Text(card, SWT.MULTI | SWT.WRAP | SWT.V_SCROLL | SWT.READ_ONLY);
        body.setText(answer == null ? "" : answer);
        body.setLayoutData(textBlockData(answer, 120, RESPONSE_MAX_HEIGHT));

        Composite actions = new Composite(card, SWT.NONE);
        actions.setLayout(new GridLayout(review.hasSuggestion() ? 2 : 1, false));
        actions.setLayoutData(fillHorizontal());

        Button copyAnswer = new Button(actions, SWT.PUSH);
        copyAnswer.setText("Copy response");
        copyAnswer.addListener(SWT.Selection, event -> copyText(answer, "Response copied"));

        if (review.hasSuggestion()) {
            Button copySuggestion = new Button(actions, SWT.PUSH);
            copySuggestion.setText("Copy suggestion");
            copySuggestion.addListener(SWT.Selection, event -> copyText(review.copyText(), "Suggested block copied for manual review"));

            Label reviewLabel = new Label(card, SWT.NONE);
            reviewLabel.setText("Suggested change");
            reviewLabel.setLayoutData(fillHorizontal());

            Text reviewBody = new Text(card, SWT.MULTI | SWT.WRAP | SWT.V_SCROLL | SWT.READ_ONLY);
            reviewBody.setText(review.displayText());
            reviewBody.setLayoutData(textBlockData(review.displayText(), 110, REVIEW_MAX_HEIGHT));
        }

        refreshTranscript();
    }

    private Composite addSystemMessage(String message) {
        Composite card = createMessageCard("System");
        Label body = new Label(card, SWT.WRAP);
        body.setText(message == null ? "" : message);
        body.setLayoutData(fillHorizontal());
        refreshTranscript();
        return card;
    }

    private void addErrorMessage(String message) {
        Composite card = createMessageCard("Error");
        Label body = new Label(card, SWT.WRAP);
        body.setText(message == null || message.isBlank() ? "Request failed." : message);
        body.setLayoutData(fillHorizontal());
        refreshTranscript();
    }

    private Composite createMessageCard(String role) {
        Composite card = new Composite(transcript, SWT.BORDER);
        GridLayout layout = new GridLayout(1, false);
        layout.marginWidth = 10;
        layout.marginHeight = 8;
        layout.verticalSpacing = 6;
        card.setLayout(layout);
        card.setLayoutData(fillHorizontal());

        Label roleLabel = new Label(card, SWT.NONE);
        roleLabel.setText(role);
        roleLabel.setLayoutData(fillHorizontal());
        return card;
    }

    private void removePendingMessage() {
        if (pendingMessage != null && !pendingMessage.isDisposed()) {
            pendingMessage.dispose();
        }
        pendingMessage = null;
        refreshTranscript();
    }

    private void refreshTranscript() {
        if (transcript == null || transcript.isDisposed()) {
            return;
        }
        transcript.layout(true, true);
        transcriptScroll.setMinSize(transcript.computeSize(SWT.DEFAULT, SWT.DEFAULT));
        transcriptScroll.layout(true, true);
        Display display = transcript.getDisplay();
        display.asyncExec(() -> {
            if (!transcriptScroll.isDisposed()) {
                transcriptScroll.setOrigin(0, transcriptScroll.getContent().getSize().y);
            }
        });
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

    private static GridData textBlockData(String value, int minHeight, int maxHeight) {
        GridData data = new GridData(SWT.FILL, SWT.TOP, true, false);
        String text = value == null ? "" : value;
        int lineCount = Math.max(1, text.split("\\R", -1).length);
        int wrappedLineEstimate = Math.max(0, text.length() / 110);
        int height = 28 + (lineCount + wrappedLineEstimate) * 18;
        data.heightHint = Math.max(minHeight, Math.min(maxHeight, height));
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
