package nl.bigbrotter.androidclient.Helpers;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

import cn.pedant.SweetAlert.SweetAlertDialog;

public class AlertHelper {

    protected static SweetAlertDialog dialog;

    public static void progress(Context context, String title) {

        if(dialog != null) {
            dismiss(false);
        }

        SweetAlertDialog pDialog = new SweetAlertDialog(context, SweetAlertDialog.PROGRESS_TYPE);
        pDialog.setTitleText(title);
        pDialog.setCancelable(false);
        pDialog.show();

        dialog = pDialog;
    }

    public static void progress(Context context) {

        progress(context, null);
    }

    public static void error(Context context, String title) {

        if(dialog != null) {

            dialog.changeAlertType(SweetAlertDialog.ERROR_TYPE);

            if(title != null) {
                dialog.setTitleText(title);
            }
        }
        else {

            SweetAlertDialog pDialog = new SweetAlertDialog(context, SweetAlertDialog.ERROR_TYPE);
            pDialog.setTitleText(title);
            pDialog.setCancelable(false);
            pDialog.show();

            pDialog.setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {

                @Override
                public void onClick(SweetAlertDialog sweetAlertDialog) {

                    dismiss();
                }
            });

            dialog = pDialog;
        }
    }

    public static void error(Context context) {

        error(context, null);
    }

    public static void dismiss(boolean animated) {

        if(dialog == null) {
            return;
        }

        if(animated) {
            dialog.dismissWithAnimation();
        }
        else {
            dialog.dismiss();
        }

        dialog = null;
    }

    public static void dismiss() {

        dismiss(true);
    }

    public static void showMessageOKCancel(Context context, String message, DialogInterface.OnClickListener okListener) {
        new AlertDialog.Builder(context)
                .setMessage(message)
                .setPositiveButton("OK", okListener)
                .setNegativeButton("Cancel", null)
                .create()
                .show();
    }
}


