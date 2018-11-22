package com.example.android.pma_inteligentna_domacnost;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

/**
 * Constructs and displays a message to the user through a Dialog.
 */
public class PushReceiverFragment extends DialogFragment {

    /**
     * PushReceiverFragment constructor.
     * @param title Title of the Alert, referenced by its resource id.
     * @param message Message contents of the Alert dialog.
     * @return The constructed AlertDialogFragment.
     */


    public static PushReceiverFragment newInstance(String title, String message) {
        PushReceiverFragment frag = new PushReceiverFragment();

        Bundle args = new Bundle();

        args.putString("title", title);
        args.putString("message", message);

        frag.setArguments(args);
        return frag;
    }



    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        String title = getArguments().getString("title");
        String message = getArguments().getString("message");

        return new AlertDialog.Builder(getActivity())
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int whichButton) {
                                // Do nothing and dismiss the dialog.
                            }
                        }
                ).create();
    }





}
