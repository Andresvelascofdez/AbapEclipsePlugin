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
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
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

    private Combo modeCombo;
    private Text questionText;
    private Text outputText;
    private Label statusLabel;
    private Text contextSummaryText;
    private Text reviewText;
    private Button askButton;
    private Button copySuggestionButton;
    private final AbapDependencyAnalyzer dependencyAnalyzer = new AbapDependencyAnalyzer();
    private final SuggestedChangeReviewBuilder reviewBuilder = new SuggestedChangeReviewBuilder();
    private final List<ConversationTurn> conversationHistory = new ArrayList<>();
    private String lastSuggestionText = "";

    @Override
    public void createPartControl(Composite parent) {
        parent.setLayout(new GridLayout(1, false));

        modeCombo = new Combo(parent, SWT.READ_ONLY);
        for (AssistantMode mode : AssistantMode.values()) {
            modeCombo.add(mode.label());
        }
        modeCombo.select(0);
        modeCombo.setLayoutData(fillHorizontal());

        Composite actions = new Composite(parent, SWT.NONE);
        actions.setLayout(new GridLayout(2, false));
        actions.setLayoutData(fillHorizontal());

        askButton = new Button(actions, SWT.PUSH);
        askButton.setText("Ask");
        askButton.addListener(SWT.Selection, event -> askAssistant());

        copySuggestionButton = new Button(actions, SWT.PUSH);
        copySuggestionButton.setText("Copy suggestion");
        copySuggestionButton.setEnabled(false);
        copySuggestionButton.addListener(SWT.Selection, event -> copySuggestion());

        Label questionLabel = new Label(parent, SWT.NONE);
        questionLabel.setText("Question");
        questionLabel.setLayoutData(fillHorizontal());

        questionText = new Text(parent, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
        questionText.setLayoutData(fillBoth(100));

        contextSummaryText = new Text(parent, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.READ_ONLY);
        contextSummaryText.setLayoutData(fillBoth(120));
        setContextSummary("Context: open ABAP/text editors will be read when you ask.");

        outputText = new Text(parent, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.READ_ONLY);
        outputText.setLayoutData(fillBoth(250));

        reviewText = new Text(parent, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.READ_ONLY);
        reviewText.setLayoutData(fillBoth(130));
        reviewText.setText("Suggested change review: no suggestion yet.");

        statusLabel = new Label(parent, SWT.NONE);
        statusLabel.setLayoutData(fillHorizontal());
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
        setContextSummary(snapshot.summaryPanel(conversationHistory.size()));
        lastSuggestionText = "";
        reviewText.setText("Suggested change review: waiting for response.");
        setBusy(true);
        outputText.setText("");
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
                        outputText.setText(response.text());
                        SuggestedChangeReview review = reviewBuilder.build(snapshot.originalExcerpt(), response.text());
                        lastSuggestionText = review.copyText();
                        reviewText.setText(review.displayText());
                        addConversationTurn(question, response.text());
                        setContextSummary(snapshot.summaryPanel(conversationHistory.size()));
                        setStatus("Done - " + response.privacyScope());
                        setBusy(false);
                    });
                    return Status.OK_STATUS;
                } catch (Exception exception) {
                    updateUi(() -> {
                        outputText.setText(exception.getMessage());
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
        modeCombo.setEnabled(!busy);
        questionText.setEnabled(!busy);
        contextSummaryText.setEnabled(!busy);
        reviewText.setEnabled(!busy);
        copySuggestionButton.setEnabled(!busy && lastSuggestionText != null && !lastSuggestionText.isBlank());
    }

    private void setStatus(String value) {
        statusLabel.setText(value == null ? "" : value);
    }

    private void copySuggestion() {
        if (lastSuggestionText == null || lastSuggestionText.isBlank()) {
            setStatus("No suggested code block available to copy");
            return;
        }
        Clipboard clipboard = new Clipboard(getSite().getShell().getDisplay());
        try {
            clipboard.setContents(new Object[] { lastSuggestionText }, new Transfer[] { TextTransfer.getInstance() });
            setStatus("Suggested block copied for manual review");
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

    private void setContextSummary(String value) {
        contextSummaryText.setText(value == null ? "" : value);
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

        private String summaryPanel(int historyTurns) {
            int characters = formatEditorContexts(openEditors).length() + formatEditorContexts(relatedContexts).length();
            StringBuilder builder = new StringBuilder();
            builder.append("Context summary:").append(System.lineSeparator());
            builder.append("- Open editors: ").append(openEditors.size()).append(System.lineSeparator());
            builder.append("- Related local sources: ").append(relatedContexts.size()).append(System.lineSeparator());
            builder.append("- References detected: ").append(analysis.references().size()).append(System.lineSeparator());
            builder.append("- Unresolved references: ").append(unresolvedReferences.size()).append(System.lineSeparator());
            builder.append("- Custom/Z objects: ").append(analysis.customReferences().size()).append(System.lineSeparator());
            builder.append("- Risk signals: ").append(analysis.riskSignals().size()).append(System.lineSeparator());
            builder.append("- Context characters: ").append(characters).append(System.lineSeparator());
            builder.append("- History turns: ").append(historyTurns);
            appendRiskSignals(builder);
            appendUnresolved(builder);
            appendCustomObjects(builder);
            return builder.toString();
        }

        private void appendRiskSignals(StringBuilder builder) {
            if (analysis.riskSignals().isEmpty()) {
                return;
            }
            builder.append(System.lineSeparator()).append(System.lineSeparator()).append("Risk signals:").append(System.lineSeparator());
            analysis.riskSignals().stream().limit(8).forEach(signal ->
                builder.append("- ").append(signal.display()).append(System.lineSeparator()));
        }

        private void appendUnresolved(StringBuilder builder) {
            if (unresolvedReferences.isEmpty()) {
                return;
            }
            builder.append(System.lineSeparator()).append("Unresolved:").append(System.lineSeparator());
            unresolvedReferences.stream().limit(8).forEach(reference ->
                builder.append("- ").append(reference).append(System.lineSeparator()));
        }

        private void appendCustomObjects(StringBuilder builder) {
            if (analysis.customReferences().isEmpty()) {
                return;
            }
            builder.append(System.lineSeparator()).append("Custom/Z objects:").append(System.lineSeparator());
            analysis.customReferences().stream().limit(8).forEach(reference ->
                builder.append("- ").append(reference.display()).append(System.lineSeparator()));
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
