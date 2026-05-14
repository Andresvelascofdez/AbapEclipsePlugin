package com.abap.assistant.ui;

import java.util.ArrayList;
import java.util.List;

import com.abap.assistant.core.AbapContextClassifier;
import com.abap.assistant.core.AssistantMode;
import com.abap.assistant.core.AssistantPromptBuilder;
import com.abap.assistant.core.AssistantRequest;
import com.abap.assistant.core.AssistantResponse;
import com.abap.assistant.core.AssistantService;
import com.abap.assistant.core.OpenAiResponsesClient;
import com.abap.assistant.core.OpenAiSettings;
import com.abap.assistant.core.SensitiveDataRedactor;
import com.abap.assistant.eclipse.EclipseDotEnvLocator;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.SWT;
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

    private Combo modeCombo;
    private Text questionText;
    private Text outputText;
    private Label statusLabel;
    private Button askButton;

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
        actions.setLayout(new GridLayout(1, false));
        actions.setLayoutData(fillHorizontal());

        askButton = new Button(actions, SWT.PUSH);
        askButton.setText("Ask");
        askButton.addListener(SWT.Selection, event -> askAssistant());

        Label questionLabel = new Label(parent, SWT.NONE);
        questionLabel.setText("Question");
        questionLabel.setLayoutData(fillHorizontal());

        questionText = new Text(parent, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
        questionText.setLayoutData(fillBoth(100));

        outputText = new Text(parent, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.READ_ONLY);
        outputText.setLayoutData(fillBoth(320));

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
        List<EditorContext> editorContexts = readOpenEditors();
        String context = trimContext(formatEditorContexts(editorContexts));

        if ((question == null || question.isBlank()) && (context == null || context.isBlank())) {
            setStatus("Enter a question or open an ABAP/text editor first");
            return;
        }

        AssistantMode mode = AssistantMode.values()[Math.max(0, modeCombo.getSelectionIndex())];
        AssistantRequest request = new AssistantRequest(mode, question, context);
        setBusy(true);
        outputText.setText("");
        setStatus("Calling OpenAI - " + editorContexts.size() + " open editor(s)");

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
    }

    private void setStatus(String value) {
        statusLabel.setText(value == null ? "" : value);
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
        return new EditorContext(editor.getTitle(), document.get());
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

        private EditorContext(String title, String text) {
            this.title = title == null || title.isBlank() ? "Untitled editor" : title;
            this.text = text == null ? "" : text;
        }

        private String title() {
            return title;
        }

        private String text() {
            return text;
        }

        private String format() {
            return "Editor: " + title + System.lineSeparator()
                + "```abap" + System.lineSeparator()
                + text
                + System.lineSeparator()
                + "```";
        }
    }
}
