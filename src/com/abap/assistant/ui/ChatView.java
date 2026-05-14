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
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
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
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.jface.text.IDocument;

public final class ChatView extends ViewPart {
    public static final String ID = "com.abap.assistant.ui.ChatView";
    private static final int MAX_CONTEXT_CHARS = 60000;

    private Combo modeCombo;
    private Text questionText;
    private Text contextText;
    private Text outputText;
    private Label statusLabel;
    private Button askButton;
    private Button useActiveEditorButton;

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
        actions.setLayout(new GridLayout(6, false));
        actions.setLayoutData(fillHorizontal());

        Button loadSelectionButton = new Button(actions, SWT.PUSH);
        loadSelectionButton.setText("Load Selection");
        loadSelectionButton.addListener(SWT.Selection, event -> loadActiveSelection());

        Button loadEditorButton = new Button(actions, SWT.PUSH);
        loadEditorButton.setText("Load Editor");
        loadEditorButton.addListener(SWT.Selection, event -> loadActiveEditor());

        Button loadOpenEditorsButton = new Button(actions, SWT.PUSH);
        loadOpenEditorsButton.setText("Load Open Editors");
        loadOpenEditorsButton.addListener(SWT.Selection, event -> loadOpenEditors());

        Button clearButton = new Button(actions, SWT.PUSH);
        clearButton.setText("Clear");
        clearButton.addListener(SWT.Selection, event -> clearConversation());

        useActiveEditorButton = new Button(actions, SWT.CHECK);
        useActiveEditorButton.setText("Use active editor");
        useActiveEditorButton.setSelection(true);

        askButton = new Button(actions, SWT.PUSH);
        askButton.setText("Ask");
        askButton.addListener(SWT.Selection, event -> askAssistant());

        Label questionLabel = new Label(parent, SWT.NONE);
        questionLabel.setText("Question");
        questionLabel.setLayoutData(fillHorizontal());

        questionText = new Text(parent, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
        questionText.setLayoutData(fillBoth(70));

        Label contextLabel = new Label(parent, SWT.NONE);
        contextLabel.setText("Context");
        contextLabel.setLayoutData(fillHorizontal());

        contextText = new Text(parent, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
        contextText.setLayoutData(fillBoth(170));

        outputText = new Text(parent, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.READ_ONLY);
        outputText.setLayoutData(fillBoth(220));

        statusLabel = new Label(parent, SWT.NONE);
        statusLabel.setLayoutData(fillHorizontal());
        setStatus("Ready");
    }

    @Override
    public void setFocus() {
        questionText.setFocus();
    }

    private void loadActiveSelection() {
        IEditorPart editor = getSite().getPage().getActiveEditor();
        ISelectionService selectionService = getSite().getWorkbenchWindow().getSelectionService();
        ISelection selection = editor == null || editor.getSite().getSelectionProvider() == null
            ? selectionService.getSelection()
            : editor.getSite().getSelectionProvider().getSelection();

        if (selection instanceof ITextSelection && !((ITextSelection) selection).getText().isBlank()) {
            ITextSelection textSelection = (ITextSelection) selection;
            contextText.setText(trimContext(formatEditorContext(editor, textSelection.getText())));
            setStatus("Selection loaded");
        } else {
            setStatus("No text selection found");
        }
    }

    private void loadActiveEditor() {
        EditorContext context = readActiveEditor();
        if (context == null) {
            setStatus("No active text editor found");
            return;
        }

        contextText.setText(trimContext(context.format()));
        setStatus("Active editor loaded - " + context.title());
    }

    private void loadOpenEditors() {
        List<EditorContext> contexts = new ArrayList<>();
        for (IEditorReference reference : getSite().getPage().getEditorReferences()) {
            IEditorPart editor = reference.getEditor(false);
            EditorContext context = readEditor(editor);
            if (context != null && !context.text().isBlank()) {
                contexts.add(context);
            }
        }

        if (contexts.isEmpty()) {
            setStatus("No open text editors found");
            return;
        }

        StringBuilder builder = new StringBuilder();
        for (EditorContext context : contexts) {
            if (builder.length() > 0) {
                builder.append(System.lineSeparator()).append(System.lineSeparator());
            }
            builder.append(context.format());
        }
        contextText.setText(trimContext(builder.toString()));
        setStatus("Open editors loaded - " + contexts.size());
    }

    private void clearConversation() {
        questionText.setText("");
        contextText.setText("");
        outputText.setText("");
        setStatus("Ready");
    }

    private void askAssistant() {
        String question = questionText.getText();
        String context = contextText.getText();
        if (useActiveEditorButton.getSelection()) {
            EditorContext editorContext = readActiveEditor();
            if (editorContext != null && !editorContext.text().isBlank()) {
                context = trimContext(editorContext.format());
                contextText.setText(context);
            }
        }

        if ((question == null || question.isBlank()) && (context == null || context.isBlank())) {
            setStatus("Enter a question or load editor context first");
            return;
        }

        AssistantMode mode = AssistantMode.values()[Math.max(0, modeCombo.getSelectionIndex())];
        AssistantRequest request = new AssistantRequest(mode, question, context);
        setBusy(true);
        outputText.setText("");
        setStatus("Calling OpenAI");

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
        contextText.setEnabled(!busy);
        useActiveEditorButton.setEnabled(!busy);
    }

    private void setStatus(String value) {
        statusLabel.setText(value == null ? "" : value);
    }

    private EditorContext readActiveEditor() {
        return readEditor(getSite().getPage().getActiveEditor());
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

    private String formatEditorContext(IEditorPart editor, String text) {
        String title = editor == null ? "selection" : editor.getTitle();
        return new EditorContext(title, text).format();
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
