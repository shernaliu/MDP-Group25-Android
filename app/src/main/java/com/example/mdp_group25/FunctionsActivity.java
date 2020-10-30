package com.example.mdp_group25;

import android.os.Bundle;
import android.view.View;
import android.content.SharedPreferences;
import android.widget.Toast;
import android.widget.EditText;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class FunctionsActivity extends AppCompatActivity {
    private static String TAG = "FunctionsActivity";
    public static final String labelOne = "labelOne";
    public static final String functionOne = "functionOne";
    public static final String labelTwo = "labelTwo";
    public static final String functionTwo = "functionTwo";
    EditText label1Input;
    EditText function1Input;
    EditText label2Input;
    EditText function2Input;
    Button saveBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_functions);
        SharedPreferences pref = getApplicationContext().getSharedPreferences("CommunicationsPreferences", 0);
        final SharedPreferences.Editor editor = pref.edit();

        // retrieve components in FunctionsActivity
        label1Input = findViewById(R.id.label1Input);
        function1Input = findViewById(R.id.function1Input);
        label2Input = findViewById(R.id.label2Input);
        function2Input = findViewById(R.id.function2Input);
        saveBtn = findViewById(R.id.saveBtn);

        // populate the functions textviews based on the data saved in FunctionsPreferences
        String functionOneData = pref.getString(functionOne, "");
        function1Input.setText(functionOneData);
        String functionTwoData = pref.getString(functionTwo, "");
        function2Input.setText(functionTwoData);

        // populate the labels textviews based on the data saved in FunctionsPreferences
        String labelOneData = pref.getString(labelOne, "");
        String labelTwoData = pref.getString(labelTwo, "");
        label1Input.setText(labelOneData);
        label2Input.setText(labelTwoData);

        saveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // retrieve input values
                String fnOneInput, fnTwoInput, labelOneInput, labelTwoInput;
                function1Input = findViewById(R.id.function1Input);
                label1Input = findViewById(R.id.label1Input);
                function2Input = findViewById(R.id.function2Input);
                label2Input = findViewById(R.id.label2Input);
                fnOneInput = function1Input.getText().toString();
                fnTwoInput = function2Input.getText().toString();
                labelOneInput = label1Input.getText().toString();
                labelTwoInput = label2Input.getText().toString();

                // clear the editor, save the values into its respective variables and commit changes
                editor.clear();
                editor.putString(labelOne, labelOneInput);
                editor.putString(functionOne, fnOneInput);
                editor.putString(labelTwo, labelTwoInput);
                editor.putString(functionTwo, fnTwoInput);
                editor.commit();

                // clear the focus for each input
                function1Input.clearFocus();
                function2Input.clearFocus();
                label1Input.clearFocus();
                label2Input.clearFocus();
                Toast.makeText(getApplicationContext(), "Saved all changes!", Toast.LENGTH_LONG).show();
            }
        });

    }
}

