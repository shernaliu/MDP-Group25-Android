package com.example.mdp_group25;


import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class CommunicationsConfig extends AppCompatActivity {

    Button savePreferences;
    EditText firstFunctionValue;
    EditText secondFunctionValue;
    EditText firstLabelValue;
    EditText secondLabelValue;

    public static final String firstFunction = "firstFunction";
    public static final String secondFunction = "secondFunction";
    public static final String firstLabel = "firstLabel";
    public static final String secondLabel="secondLabel";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_communications_config);

        SharedPreferences pref = getApplicationContext().getSharedPreferences("CommunicationsPreferences", 0);
        final SharedPreferences.Editor editor = pref.edit();

        //Set the Text Field According to Function values saved in the Preferences
        firstFunctionValue = findViewById(R.id.firstFunctionValue);
        secondFunctionValue = findViewById(R.id.secondFunctionValue);
        String savedFirstFunction = pref.getString(firstFunction, "");
        String savedSecondFunction = pref.getString(secondFunction, "");
        firstFunctionValue.setText(savedFirstFunction);
        secondFunctionValue.setText(savedSecondFunction);

        firstLabelValue = findViewById(R.id.firstLabelValue);
        secondLabelValue = findViewById(R.id.secondLabelValue);
        String savedFirstLabel = pref.getString(firstLabel, "");
        String savedSecondLabel = pref.getString(secondLabel, "");
        firstLabelValue.setText(savedFirstLabel);
        secondLabelValue.setText(savedSecondLabel);


        savePreferences = findViewById(R.id.savePreferences);
        savePreferences.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //get the input value
                String firstValueToSave;
                String secondValueToSave;
                String firstLabelToSave;
                String secondLabelToSave;
                firstFunctionValue = findViewById(R.id.firstFunctionValue);
                firstLabelValue = findViewById(R.id.firstLabelValue);
                secondFunctionValue = findViewById(R.id.secondFunctionValue);
                secondLabelValue = findViewById(R.id.secondLabelValue);
                firstValueToSave = firstFunctionValue.getText().toString();
                secondValueToSave = secondFunctionValue.getText().toString();

                firstLabelToSave = firstLabelValue.getText().toString();
                secondLabelToSave = secondLabelValue.getText().toString();

                editor.clear();

                editor.putString(firstLabel, firstLabelToSave);
                editor.putString(firstFunction, firstValueToSave);
                editor.putString(secondLabel, secondLabelToSave);
                editor.putString(secondFunction, secondValueToSave);

                editor.commit();

                firstFunctionValue.clearFocus();
                secondFunctionValue.clearFocus();
                firstLabelValue.clearFocus();
                secondLabelValue.clearFocus();
                Toast.makeText(getApplicationContext(), "Saved", Toast.LENGTH_LONG).show();
            }
        });

    }
}

