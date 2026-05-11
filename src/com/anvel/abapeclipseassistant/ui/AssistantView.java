package com.anvel.abapeclipseassistant.ui;

import com.anvel.abapeclipseassistant.core.AbapContextClassifier;
import com.anvel.abapeclipseassistant.core.AssistantMode;
import com.anvel.abapeclipseassistant.core.AssistantPromptBuilder;
import com.anvel.abapeclipseassistant.core.AssistantRequest;
import com.anvel.abapeclipseassistant.core.AssistantResponse;
import com.anvel.abapeclipseassistant.core.AssistantService;
import com.anvel.abapeclipseassistant.core.OpenAiResponsesClient;
import com.anvel.abapeclipseassistant.core.OpenAiSettings;
import com.anvel.abapeclipseassistant.core.SensitiveDataRedactor;

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
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.part.ViewPart;

public final class AssistantView extends ViewPart {
    public static final String ID = "com.anvel.abapeclipseassistant.views.assistant";

    private Combo modeCombo;
    private Text inputText;
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
        actions.setLayout(new GridLayout(2, false));
        actions.setLayoutData(fillHorizontal());

        Button loadSelectionButton = new Button(actions, SWT.PUSH);
        loadSelectionButton.setText("Load Selection");
        loadSelectionButton.addListener(SWT.Selection, event -> loadActiveSelection());

        askButton = new Button(actions, SWT.PUSH);
        askButton.setText("Ask");
        askButton.addListener(SWT.Selection, event -> askAssistant());

        inputText = new Text(parent, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
        inputText.setLayoutData(fillBoth(140));

        outputText = new Text(parent, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.READ_ONLY);
        outputText.setLayoutData(fillBoth(220));

        statusLabel = new Label(parent, SWT.NONE);
        statusLabel.setLayoutData(fillHorizontal());
        setStatus("Ready");
    }

    @Override
    public void setFocus() {
        inputText.setFocus();
    }

    private void loadActiveSelection() {
        IEditorPart editor = getSite().getPage().getActiveEditor();
        ISelectionService selectionService = getSite().getWorkbenchWindow().getSelectionService();
        ISelection selection = editor == null ? selectionService.getSelection() : editor.getSite().getSelectionProvider().getSelection();

        if (selection instanceof ITextSelection textSelection && !textSelection.getText().isBlank()) {
            inputText.setText(textSelection.getText());
            setStatus("Selection loaded");
        } else {
            setStatus("No text selection found");
        }
    }

    private void askAssistant() {
        String input = inputText.getText();
        if (input == null || input.isBlank()) {
            setStatus("Enter a question or load a selection first");
            return;
        }

        AssistantMode mode = AssistantMode.values()[Math.max(0, modeCombo.getSelectionIndex())];
        AssistantRequest request = new AssistantRequest(mode, input, "");
        setBusy(true);
        outputText.setText("");
        setStatus("Calling OpenAI");

        Job job = new Job("ABAP Eclipse Assistant request") {
            @Override
            protected IStatus run(IProgressMonitor monitor) {
                try {
                    AssistantService service = new AssistantService(
                        new AssistantPromptBuilder(new SensitiveDataRedactor(), new AbapContextClassifier()),
                        new OpenAiResponsesClient(OpenAiSettings.fromEnvironment()));
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
                    return new Status(IStatus.ERROR, "com.anvel.abapeclipseassistant", exception.getMessage(), exception);
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
        inputText.setEnabled(!busy);
    }

    private void setStatus(String value) {
        statusLabel.setText(value == null ? "" : value);
    }

    private static GridData fillHorizontal() {
        return new GridData(SWT.FILL, SWT.CENTER, true, false);
    }

    private static GridData fillBoth(int heightHint) {
        GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
        data.heightHint = heightHint;
        return data;
    }
}

