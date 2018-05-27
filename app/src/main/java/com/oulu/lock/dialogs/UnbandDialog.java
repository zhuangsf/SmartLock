package com.oulu.lock.dialogs;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.oulu.lock.R;

/**
 * Created by admin on 2017/9/11.
 */

public class UnbandDialog extends Dialog {
    private int mUnbandId = -1;

    public UnbandDialog(Context context) {
        super(context);
    }

    public UnbandDialog(Context context, int themeResId) {
        super(context, themeResId);
    }

    protected UnbandDialog(Context context, boolean cancelable, OnCancelListener cancelListener) {
        super(context, cancelable, cancelListener);
    }

    public void setUnbandId(int unband){
        mUnbandId = unband;
    }

    public int getUnbandId(){
        return mUnbandId;
    }

    public static class Builder {
        private Context context;
        private String title;
        private String positiveButtonText;
        private String negativeButtonText;
        private OnClickListener positiveButtonClickListener;
        private OnClickListener negativeButtonClickListener;
        private View layout;

        public Builder(Context context) {
            this.context = context;
            LayoutInflater inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            layout = inflater.inflate(R.layout.dialog_unband, null);
        }

        /**
         * Set the Dialog title from resource
         *
         * @param title
         * @return
         */
        public Builder setTitle(int title) {
            this.title = (String) context.getText(title);
            return this;
        }

        public Builder setTitle(String title) {
            this.title = title;
            return this;
        }


        /**
         * Set the positive button resource and it's listener
         *
         * @param positiveButtonText
         * @return
         */
        public Builder setPositiveButton(int positiveButtonText,
                                         OnClickListener listener) {
            this.positiveButtonText = (String) context
                    .getText(positiveButtonText);
            this.positiveButtonClickListener = listener;
            return this;
        }

        public Builder setPositiveButton(String positiveButtonText,
                                         OnClickListener listener) {
            this.positiveButtonText = positiveButtonText;
            this.positiveButtonClickListener = listener;
            return this;
        }

        public Builder setNegativeButton(int negativeButtonText,
                                         OnClickListener listener) {
            this.negativeButtonText = (String) context
                    .getText(negativeButtonText);
            this.negativeButtonClickListener = listener;
            return this;
        }

        public Builder setNegativeButton(String negativeButtonText,
                                         OnClickListener listener) {
            this.negativeButtonText = negativeButtonText;
            this.negativeButtonClickListener = listener;
            return this;
        }

        public UnbandDialog create() {

            // instantiate the dialog with the custom Theme
            final UnbandDialog dialog = new UnbandDialog(context, R.style.no_border_dialog);

            dialog.addContentView(layout, new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.FILL_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            // set the dialog title
            ((TextView) layout.findViewById(R.id.title)).setText(title);
            // set the confirm button
            if (positiveButtonText != null) {
                ((TextView) layout.findViewById(R.id.positiveButton))
                        .setText(positiveButtonText);
                if (positiveButtonClickListener != null) {
                    ((TextView) layout.findViewById(R.id.positiveButton))
                            .setOnClickListener(new View.OnClickListener() {
                                public void onClick(View v) {
                                    positiveButtonClickListener.onClick(dialog,
                                            DialogInterface.BUTTON_POSITIVE);
                                }
                            });
                }
            } else {
                // if no confirm button just set the visibility to GONE
                layout.findViewById(R.id.positiveButton).setVisibility(
                        View.GONE);
            }
            // set the cancel button
            if (negativeButtonText != null) {
                ((TextView) layout.findViewById(R.id.negativeButton))
                        .setText(negativeButtonText);
                if (negativeButtonClickListener != null) {
                    ((TextView) layout.findViewById(R.id.negativeButton))
                            .setOnClickListener(new View.OnClickListener() {
                                public void onClick(View v) {
                                    negativeButtonClickListener.onClick(dialog,
                                            DialogInterface.BUTTON_NEGATIVE);
                                }
                            });
                }
            } else {
                // if no confirm button just set the visibility to GONE
                layout.findViewById(R.id.negativeButton).setVisibility(
                        View.GONE);
            }
            dialog.setContentView(layout);
            return dialog;
        }
    }
}
