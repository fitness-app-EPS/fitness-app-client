package com.fitnessapp.client;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.fitnessapp.client.Utils.StaticStrings;
import com.fitnessapp.client.Utils.User;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.regex.Pattern;

public class RegisterActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;

    private EditText username, password, email, address, telNum;
    private Spinner role, speciality;
    private UrlConnectorCreateClient uccc;
    private UrlConnectorCreateTrainer trainerData;
    private SpinnerConnector sp;
    private HttpURLConnection conn, getConn;
    ArrayList<String> specs = new ArrayList<String>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register);
        mAuth = FirebaseAuth.getInstance();

        username = findViewById(R.id.editTextUser);
        password = findViewById(R.id.editTextPwd);
        email = findViewById(R.id.editTextEmail);
        address = findViewById(R.id.editTextAddres);
        telNum = findViewById(R.id.editTextTelNum);
        role = findViewById(R.id.spinnerRoles);
        speciality = findViewById(R.id.spinnerSpec);

        sp = new SpinnerConnector();
        sp.execute();

        ArrayAdapter<CharSequence> adapterRoles = ArrayAdapter.createFromResource(this, R.array.rolesOptions, android.R.layout.simple_spinner_item);

        adapterRoles.setDropDownViewResource(R.layout.spinner_item);
        role.setAdapter(adapterRoles);

        Button buttonBack = findViewById(R.id.buttonBack);

        buttonBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                back(v);
            }
        });

        Button buttonRegister = findViewById(R.id.buttonRegister);

        buttonRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                register(v);
            }
        });
    }

    @Override
    public void onBackPressed() {}

    public void back(View view) {

        Intent intent = new Intent(this, LoginActivity.class);
        startActivity(intent);
        finish();

    }

    public void register(View view) {
        final String emailText = email.getText().toString();
        final String passwordText = password.getText().toString();
        final String nameText  = username.getText().toString();
        final String roleText = role.getSelectedItem().toString();
        if(!emailText.equals("") && !passwordText.equals("") && validateEmailFormat(emailText) && validatePassword(passwordText) && roleText != null) {
            User userObj = new User(nameText, emailText, passwordText, roleText, null);
            userObj.setAddress(address.getText().toString());
            userObj.setTelNum(telNum.getText().toString());
            final User userObj2 = userObj;
            if (roleText.equals("Trainer")) {
                mAuth.createUserWithEmailAndPassword(emailText, passwordText)
                        .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                            @Override
                            public void onComplete(@NonNull Task<AuthResult> task) {
                                if (task.isSuccessful()) {
                                    // Sign in success, update UI with the signed-in user's information
                                    Log.d("REGISTER: ", "createUserWithEmail:success");
                                    FirebaseUser user = mAuth.getCurrentUser();
                                    DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();
                                    mDatabase.child("Users").child(user.getUid()).setValue(userObj2);
                                    uccc = new UrlConnectorCreateClient();
                                    uccc.execute();
                                    try {
                                        Thread.sleep(1000);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                    trainerData = new UrlConnectorCreateTrainer();
                                    trainerData.execute();
                                    Intent i = new Intent(RegisterActivity.this, BaseDrawerActivity.class);
                                    Bundle b = new Bundle();
                                    b.putSerializable("userType", userObj2.getRole());
                                    b.putString("userEmail",emailText);
                                    i.putExtra("bundle", b);
                                    startActivity(i);
                                    finish();

                                } else {
                                    // If sign in fails, display a message to the user.
                                    Log.w("REGISTER: ", "createUserWithEmail:failure", task.getException());
                                    Toast.makeText(RegisterActivity.this, "Authentication failed.",
                                            Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
            } else {
                mAuth.signOut();
                Intent in = new Intent(RegisterActivity.this, QuestionActivity.class);
                Bundle b = new Bundle();
                b.putSerializable("user", userObj);
                b.putString("userEmail",emailText);
                in.putExtra("b", b);
                startActivity(in);
                finish();
            }
        } else {
            Toast.makeText(RegisterActivity.this, getText(R.string.inputData),
                    Toast.LENGTH_SHORT).show();

        }
    }

    private boolean validatePassword(String passwordText) {
        if(passwordText.length() < 6){
            Toast.makeText(RegisterActivity.this, getString(R.string.shortPassword), Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private boolean validateEmailFormat(String email) {
        Pattern pattern = Patterns.EMAIL_ADDRESS;
        if(!pattern.matcher(email).matches()){
            Toast.makeText(RegisterActivity.this, getString(R.string.invalidEmail), Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    private class UrlConnectorCreateTrainer extends AsyncTask<Void,Void,Void> {

        @Override
        protected Void doInBackground(Void... params) {
            try {
                JSONObject rec = new JSONObject();

                // Get trainer spec's JSON
                URL url = new URL(StaticStrings.ipserver + "/speciality/findBySpecialityName/" + speciality.getSelectedItem().toString());
                getConn = (HttpURLConnection) url.openConnection();
                getConn.setRequestMethod("GET");
                getConn.setRequestProperty("Accept", "application/json");
                System.out.println(url);
                System.out.println(getConn.getResponseCode());

                if (getConn.getResponseCode() == 200 || getConn.getResponseCode() == 204) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(getConn.getInputStream()));
                    String output = br.readLine();
                    JSONArray arr = new JSONArray(output);

                    rec = arr.getJSONObject(0);
                    System.out.println(rec);
                }

                // Create trainer in the DB
                URL url2 = new URL(StaticStrings.ipserver + "/trainer/");
                conn = (HttpURLConnection) url2.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                String jsonString = new JSONObject()
                        .put("userName", username.getText())
                        .put("userPassword", password.getText())
                        .put("mail", email.getText())
                        .put("specialityId", rec)
                        .put("telephone", telNum.getText())
                        .put("address", address.getText())
                        .toString();

                OutputStreamWriter os2 = new OutputStreamWriter(conn.getOutputStream());
                os2.write(jsonString);
                os2.flush();
                os2.close();
                System.out.println(jsonString);
            } catch (Exception e) {
                System.out.println("User could not be created: ");
                e.printStackTrace();
            }
            return null;
        }
    }
    private class UrlConnectorCreateClient extends AsyncTask<Void,Void,Void> {

        @Override
        protected Void doInBackground(Void... params) {
            try {
                //CREATE CLIENT IN DB
                URL url = new URL(StaticStrings.ipserver + "/trainer/");
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);

                String jsonString = new JSONObject()
                        .put("userName", username.getText().toString())
                        .put("userPassword", password.getText().toString())
                        .put("mail", email.getText().toString())
                        .put("telephone", telNum.getText().toString())
                        .put("address", address.getText().toString())
                        .toString();

                OutputStream os = conn.getOutputStream();
                os.write(jsonString.getBytes());
                os.flush();
                os.close();
                System.out.println("CONNECTION CODE: " + conn.getResponseCode());
                conn.disconnect();
            } catch (Exception e) {
                System.out.println("User could not be created: ");
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            super.onPostExecute(result);
        }
    }

        private class SpinnerConnector extends AsyncTask<Void, Void, Void>{

            ArrayAdapter<String> dataSpecAdapter;

            @Override
            protected Void doInBackground(Void... voids) {
                try{
                    URL url = new URL(StaticStrings.ipserver  + "/speciality/");
                    getConn = (HttpURLConnection) url.openConnection();
                    getConn.setRequestMethod("GET");
                    getConn.setRequestProperty("Accept", "application/json");
                    System.out.println(url);
                    System.out.println(getConn.getResponseCode());

                    if(getConn.getResponseCode() == 200 || getConn.getResponseCode() == 204) {
                        BufferedReader br = new BufferedReader(new InputStreamReader(getConn.getInputStream()));
                        String output = br.readLine();
                        JSONArray arr = new JSONArray(output);

                        for(int i = 0; i < arr.length(); i++){
                            JSONObject obj = arr.getJSONObject(i);
                            specs.add(obj.getString("specialityName"));
                        }
                    }

                    dataSpecAdapter = new ArrayAdapter<String>(RegisterActivity.this,
                            android.R.layout.simple_spinner_item, specs);
                    dataSpecAdapter.setDropDownViewResource(R.layout.spinner_item);
                }catch(Exception e){
                    e.printStackTrace();
                }

                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                speciality.setAdapter(dataSpecAdapter);
                super.onPostExecute(result);
            }
        }
}
