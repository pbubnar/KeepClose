package com.keepcloseapp.keepclose;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;

import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.dd.CircularProgressButton;
import com.amazonaws.mobileconnectors.lambdainvoker.*;

import com.google.gson.*;

public class Login extends AppCompatActivity {
    CognitoCachingCredentialsProvider credentialsProvider;
    CircularProgressButton loginProgress;
    LambdaInvokerFactory factory;
    LoginInterface loginInterface;
    JsonObject loginData;
    SharedPreferences preferences;
    EditText userEditText;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        if(preferences.getBoolean("loggedIn",false))
            launchMainApp();

        createLogin();
        textCheck();
        initializeAmazonComponents();
        initializeLambda();

    }

    public void createLogin()
    {
        setContentView(R.layout.activity_login);
        initializeAmazonComponents();




        loginProgress = (CircularProgressButton) findViewById(R.id.btnLogin);

        loginProgress.setIndeterminateProgressMode(true);
        loginProgress.setText("Log In");
        loginProgress.setCompleteText("Logged in");
        loginProgress.setErrorText("Log in Failed");
        loginProgress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                LoginEventRequest request = new LoginEventRequest();
                userEditText = (EditText)findViewById(R.id.txtUsername);
                EditText passEditText = (EditText)findViewById(R.id.txtPass);
                request.setUsername(userEditText.getText().toString());
                Log.w("TEXTCHECK", userEditText.getText().toString());
                request.setPassword(passEditText.getText().toString());

                new AsyncTask<LoginEventRequest, Void, JsonObject>() {
                    @Override
                    protected void onPreExecute()
                    {
                        loginProgress.setProgress(50);
                        findViewById(R.id.txtAuth).setVisibility(View.VISIBLE);
                    }

                    @Override
                    protected JsonObject doInBackground(LoginEventRequest... params) {
                        try {
                            Log.wtf("jesus take the wheel", "Im going in blind");
                            return loginInterface.LambdaKCLogin(params[0]);
                        } catch (LambdaFunctionException lfe) {
                            Log.e("Tag", "Failed to invoke login", lfe);
                            Log.e("deetz",lfe.getDetails());
                            return null;
                        }

                    }

                    @Override
                    protected void onPostExecute(JsonObject result) {
                        if (result == null) {
                            return;
                        }

                        loginData = result;
                        Log.wtf("RESULT OF QUERY", loginData.toString());
                        loginProgress.setProgress(100);
                        findViewById(R.id.txtAuth).setVisibility(View.INVISIBLE);
                        SharedPreferences.Editor editor = preferences.edit();
                        editor.putString("Username", userEditText.getText().toString());
                        editor.putBoolean("loggedIn",true);
                        editor.commit();
                        launchMainApp();




                    }
                }.execute(request);
            }
        });


    }

    public void initializeAmazonComponents() {
        // Initialize the Amazon Cognito credentials provider
                credentialsProvider = new CognitoCachingCredentialsProvider(
                this.getApplicationContext(),
                "us-east-1:0afc869f-b0d3-4649-9841-0044fd0ade19", // Identity Pool ID
                Regions.US_EAST_1 // Region
        );


    }

    public void textCheck()
    {
        final EditText userText =(EditText) findViewById(R.id.txtUsername);
        final EditText passText =(EditText) findViewById(R.id.txtPass);

        userText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length()>0)
                {
                    userText.setGravity(Gravity.LEFT);
                }
                else {userText.setGravity(Gravity.CENTER);}
            }

        });
        passText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void afterTextChanged(Editable s) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length()>0)
                {
                    passText.setGravity(Gravity.LEFT);
                }
                else {passText.setGravity(Gravity.CENTER);}
            }


        });
    }

    public void initializeLambda()
    {
        factory = new LambdaInvokerFactory(this.getApplicationContext(),
            Regions.US_EAST_1, credentialsProvider);

        loginInterface = factory.build(LoginInterface.class);
    }

    public void launchMainApp()
    {
        Intent intent = new Intent(this, Main.class);
        startActivity(intent);
    }
}
