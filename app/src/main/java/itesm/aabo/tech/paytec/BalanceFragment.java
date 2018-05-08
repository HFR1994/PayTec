package itesm.aabo.tech.paytec;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Objects;


/**
 * A simple {@link Fragment} subclass.
 */
public class BalanceFragment extends Fragment {

    NavigationView navigationView = null;
    TextView currentBalace;
    EditText writeText;
    Button boton;
    static String writeKey;

    public BalanceFragment() {}

    public static String getWriteKey() {
        return writeKey;
    }

    public static void setWriteKey(String writeKey) {
        BalanceFragment.writeKey = writeKey.toUpperCase();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, final ViewGroup container,
                             Bundle savedInstanceState) {

        final View rootView =  inflater.inflate(R.layout.fragment_balance, container, false);

        currentBalace = ((TextView) rootView.findViewById(R.id.textView5));
        writeText = ((EditText) rootView.findViewById(R.id.editText));
        boton = ((Button) rootView.findViewById(R.id.button));

        final Dialog commentDialog = new Dialog(rootView.getContext());
        commentDialog.setCancelable(false);
        commentDialog.setContentView(R.layout.reply);
        Button okBtn = (Button) commentDialog.findViewById(R.id.ok);
        okBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                EditText textEdit = (EditText) commentDialog.findViewById(R.id.password_text);

                setWriteKey(textEdit.getText().toString());

                if(getWriteKey().length() == 0){
                    setWriteKey("FFFFFFFFFFFF");
                    ((MainActivity) Objects.requireNonNull(getActivity())).readtoRead();
                    commentDialog.dismiss();
                }else {
                    if (getWriteKey().length() != 12) {
                        Toast.makeText(rootView.getContext(),
                                "La llave que ingresate no concuerda con lo esperado",
                                Toast.LENGTH_LONG).show();
                    } else {
                        ((MainActivity) Objects.requireNonNull(getActivity())).readtoRead();
                        commentDialog.dismiss();
                    }
                }

            }
        });
        Button cancelBtn = (Button) commentDialog.findViewById(R.id.cancel);
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navigationView = (NavigationView) rootView.getRootView().findViewById(R.id.nav_view);
                navigationView.getMenu().getItem(0).setChecked(true);
                MainFragment fragment = new MainFragment();
                android.support.v4.app.FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
                fragmentTransaction.replace(R.id.fragment_container, fragment);
                fragmentTransaction.commit();
                commentDialog.dismiss();
            }
        });

        if(!commentDialog.isShowing()){
            commentDialog.show();
        }

        boton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((MainActivity) Objects.requireNonNull(getActivity())).readtoWrite();
                commentDialog.dismiss();
            }
        });

        // Inflate the layout for this fragment

        return rootView;
    }

}
