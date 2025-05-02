package sk.mizik.enviromon;

import static android.content.Context.MODE_PRIVATE;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.fragment.app.DialogFragment;

public class IdDialog extends DialogFragment {
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        String[] options = {"OFFICE", "BEDROOM", "BATHROOM", "LIVINGROOM", "HALL", "CELLAR", "OUTDOOR"};
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder
                .setTitle(R.string.id_dialog_title)
                .setItems(options, (dialog, selected) -> {
                    SharedPreferences.Editor spTd = getActivity().getSharedPreferences(MainActivity.TAG, MODE_PRIVATE).edit();
                    spTd.putString(MainActivity.ID, options[selected]);
                    spTd.apply();
                });
        return builder.create();
    }
}
