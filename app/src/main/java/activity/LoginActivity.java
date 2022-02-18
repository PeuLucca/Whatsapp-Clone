package activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.example.whatsapp.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;

import config.ConfiguracaoFirebase;
import model.Usuario;

public class LoginActivity extends AppCompatActivity {

    private EditText campoEmail,campoSenha;
    private FirebaseAuth autenticacao;
    private Usuario usuario;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        campoEmail = findViewById(R.id.editEmailLogin);
        campoSenha = findViewById(R.id.editSenhaLogin);

        autenticacao = ConfiguracaoFirebase.getFirebaseAutenticacao();
    }

    @Override
    protected void onStart() {
        super.onStart();

        FirebaseUser usuarioAtual = autenticacao.getCurrentUser();
        if(usuarioAtual != null){
            abrirTelaPrincipal();
        }
    }

    public void logarUsuario(){

        autenticacao.signInWithEmailAndPassword(usuario.getEmail(), usuario.getSenha())
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {

                if( task.isSuccessful() ){

                    abrirTelaPrincipal();

                }else if(!task.isSuccessful()){

                    String excecao = "";
                    try {
                        throw task.getException();
                    }catch(FirebaseAuthInvalidUserException e){
                        excecao = "Usuário não cadastrado";
                    }catch (FirebaseAuthInvalidCredentialsException e){
                        excecao = "E-mail e senha não correspondentes";
                    }catch (Exception e){
                        excecao = "Erro ao fazer login: " + e.getMessage();
                        e.printStackTrace();
                    }
                    Toast.makeText(getApplicationContext(),excecao,Toast.LENGTH_SHORT).show();
                    campoEmail.setText("");
                    campoSenha.setText("");
                }

            }
        });

    }

    public void validarAutenticacaoUsuario(View view){

        String email = campoEmail.getText().toString();
        String senha = campoSenha.getText().toString();

        if(!email.isEmpty()){

            if( !senha.isEmpty() ){

                usuario = new Usuario();
                usuario.setEmail( email );
                usuario.setSenha( senha );

                logarUsuario();

            }else {
                Toast.makeText(getApplicationContext(),"Insira a senha",Toast.LENGTH_SHORT).show();
            }

        }else {
            Toast.makeText(getApplicationContext(),"Insira o email",Toast.LENGTH_SHORT).show();
        }

    }

    public void abrirTelaCadastro(View view){

        Intent intent = new Intent( LoginActivity.this, CadastroActivity.class );
        startActivity( intent );
    }

    public void abrirTelaPrincipal(){

        Intent intent = new Intent( LoginActivity.this, MainActivity.class );
        startActivity( intent );
    }

}