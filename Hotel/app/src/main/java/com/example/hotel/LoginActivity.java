package com.example.hotel;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.CycleInterpolator;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;

/**
 * Кіру және тіркелу экраны. Логин мен құпиясөз users кестесінде тексеріледі,
 * сессия SharedPreferences ішінде сақталады. Рөл: admin немесе client.
 */
public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "HOTEL_LOGIN";

    private DbHelper db;
    private boolean registerMode;
    private EditText editName, editUser, editPass;
    private TextInputLayout tilName;
    private TextView txtTitle, txtSub, txtDemo;
    private MaterialButton btnLogin, btnSwitch;
    private View box;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Сессия бар болса, кіру экранын өткізіп жібереміз
        if (Session.current(this) != null) {
            openMain();
            return;
        }

        setContentView(R.layout.activity_login);
        Ui.applyInsets(findViewById(R.id.root), findViewById(R.id.root));

        db = new DbHelper(this);
        box = findViewById(R.id.loginBox);
        tilName = findViewById(R.id.tilName);
        editName = findViewById(R.id.editName);
        editUser = findViewById(R.id.editUser);
        editPass = findViewById(R.id.editPass);
        txtTitle = findViewById(R.id.txtLoginTitle);
        txtSub = findViewById(R.id.txtLoginSub);
        txtDemo = findViewById(R.id.txtDemo);
        btnLogin = findViewById(R.id.btnLogin);
        btnSwitch = findViewById(R.id.btnSwitch);

        btnLogin.setOnClickListener(v -> submit());
        btnSwitch.setOnClickListener(v -> setMode(!registerMode));

        if (Ui.animationsOn(this)) {
            box.setAlpha(0f);
            box.setTranslationY(40f);
            box.animate().alpha(1f).translationY(0f).setDuration(600).start();
        }
    }

    private void setMode(boolean register) {
        registerMode = register;
        tilName.setVisibility(register ? View.VISIBLE : View.GONE);
        txtTitle.setText(register ? "Тіркелу" : "Қонақүй");
        txtSub.setText(register
                ? "Жаңа клиент аккаунтын жасаңыз. Рөлі: клиент."
                : "Нөмірлер мен брондауларды есепке алу. Жалғастыру үшін жүйеге кіріңіз.");
        btnLogin.setText(register ? "Аккаунт жасау" : "Кіру");
        btnSwitch.setText(register ? "Кіру экранына оралу" : "Тіркелу");
        txtDemo.setVisibility(register ? View.GONE : View.VISIBLE);
    }

    private void submit() {
        String username = editUser.getText().toString().trim();
        String password = editPass.getText().toString();

        if (username.isEmpty() || password.isEmpty()) {
            fail("Логин мен құпиясөзді жазыңыз");
            return;
        }

        if (registerMode) {
            String name = editName.getText().toString().trim();
            if (name.isEmpty()) {
                fail("Аты-жөніңізді жазыңыз");
                return;
            }
            if (username.length() < 3) {
                fail("Логин кемінде 3 таңба болуы керек");
                return;
            }
            if (password.length() < 4) {
                fail("Құпиясөз кемінде 4 таңба болуы керек");
                return;
            }
            if (db.usernameExists(username)) {
                fail("Мұндай логин бар: " + username);
                return;
            }
            if (db.register(username, password, name) == -1) {
                fail("Тіркелу сәтсіз аяқталды");
                return;
            }
            Toast.makeText(this, "Аккаунт жасалды", Toast.LENGTH_SHORT).show();
        }

        User u = db.login(username, password);
        if (u == null) {
            fail("Логин немесе құпиясөз қате");
            return;
        }
        Session.save(this, u);
        Log.i(TAG, "Кірді: " + u.getUsername() + ", рөлі " + u.getRole());
        openMain();
    }

    /** Қате: хабарлама және өрістің «шайқалуы» (қасиет анимациясы). */
    private void fail(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        Log.w(TAG, message);
        if (Ui.animationsOn(this)) {
            ObjectAnimator a = ObjectAnimator.ofFloat(box, "translationX", 0f, 22f);
            a.setInterpolator(new CycleInterpolator(3));
            a.setDuration(380);
            a.start();
        }
    }

    private void openMain() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}
