package org.openlmis.core.view.widget;

import android.app.Dialog;
import android.app.FragmentManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import org.openlmis.core.R;

import lombok.Getter;
import lombok.Setter;
import roboguice.fragment.provided.RoboDialogFragment;
import roboguice.inject.InjectView;

public class SignatureDialog extends RoboDialogFragment implements View.OnClickListener {

    @Getter
    @Setter
    DialogDelegate delegate;
    private View contentView;

    @InjectView(R.id.btn_cancel)
    public TextView btnCancel;

    @InjectView(R.id.btn_done)
    public Button btnSign;

    @InjectView(R.id.et_signature)
    public EditText etSignature;

    @InjectView(R.id.ly_signature)
    public TextInputLayout lySignature;

    @InjectView(R.id.tv_signature_title)
    public TextView tvSignatureTitle;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        contentView = inflater.inflate(R.layout.dialog_inventory_signature, container, false);
        return contentView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        initUI();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();
        setDialogAttributes();
    }

    private void initUI() {
        Bundle arguments = getArguments();
        if (arguments != null) {
            tvSignatureTitle.setText(arguments.getString("title"));
        }
        btnCancel.setOnClickListener(this);
        btnSign.setOnClickListener(this);
    }

    private void setDialogAttributes() {
        WindowManager.LayoutParams params = new WindowManager.LayoutParams();
        params.copyFrom(getDialog().getWindow().getAttributes());
        params.width = (int) (getDialog().getContext().getResources().getDisplayMetrics().widthPixels * 0.8);
        getDialog().getWindow().setAttributes(params);
    }

    private boolean checkSignature(String signature) {
        return signature.length() >= 2 && signature.matches("[a-zA-Z._]+");
    }

    @Override
    public void onClick(View v) {
        if (delegate == null) {
            return;
        }

        if (v.getId() == R.id.btn_done) {
            String signature = etSignature.getText().toString().trim();
            if (checkSignature(signature)) {
                delegate.onSign(signature);
                dismiss();
            } else {
                lySignature.setError("Signature not valid");
            }
        } else {
            delegate.onCancel();
            dismiss();
        }
    }

    public interface DialogDelegate {
        void onCancel();

        void onSign(String sign);
    }

    public static Bundle getBundleToMe(String title) {
        Bundle bundle = new Bundle();
        bundle.putString("title", title);
        return bundle;
    }

    public void show(FragmentManager manager) {
        super.show(manager, "signature_dialog");
    }

}
