package robertpopa.com.stormy;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;

/**
 * Created by RobertP on 11/07/16.
 */
public class AlertDialogFragment extends DialogFragment {

    private String mMessage;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if ( mMessage == null ) {
            this.setMessage(getActivity().getResources().getString(R.string.alert_dialog_message));
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity())
                .setTitle(R.string.alert_dialog_title)
                .setMessage(mMessage)
                .setPositiveButton(R.string.alert_dialog_ok_button_text, null);
        return builder.create();
    }

    public void setMessage(String message) {
        this.mMessage = message;
    }
}
