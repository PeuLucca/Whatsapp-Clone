package activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.example.whatsapp.R;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;

import config.ConfiguracaoFirebase;
import de.hdodenhof.circleimageview.CircleImageView;
import helper.Permissao;
import helper.UsuarioFirebase;
import model.Usuario;

public class ConfiguracoesActivity extends AppCompatActivity {

    private ImageButton imgCamera,imgGaleria;
    private static final int SELECAO_CAMERA = 100;
    private static final int SELECAO_GALERIA = 10;
    private ImageView imgEdit;
    private CircleImageView circleImageViewPerfil;
    private EditText editPerfilNome;
    private StorageReference storageReference;
    private String idUsuario;
    private Usuario usuarioLogado;

    private String[] permissoesNecessarias = new String[]{
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_configuracoes );

        // Configuracoes iniciais
        storageReference = ConfiguracaoFirebase.getFirebaseStorage();
        idUsuario = UsuarioFirebase.getIdentificadorUsuario();
        usuarioLogado = UsuarioFirebase.getDadosUsuarioLogado();


        // validar permissões:
        Permissao.validarPermissoes(permissoesNecessarias, this, 1);

        imgCamera = findViewById(R.id.imageButtonCamera);
        imgGaleria = findViewById(R.id.imageButtonGaleria);
        imgEdit = findViewById(R.id.imagemAtualizarNome);
        circleImageViewPerfil = findViewById(R.id.circleImageViewFotoPerfil);
        editPerfilNome = findViewById(R.id.editNomeConfiguracoes);

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("Configurações");
        setSupportActionBar( toolbar ); // p/ ter suporte a versoes anteriores do android

        getSupportActionBar().setDisplayHomeAsUpEnabled( true ); // icone voltar

        // recuperar dados do usuario:
        FirebaseUser usuario = UsuarioFirebase.getUsuarioAtual();
        Uri url = usuario.getPhotoUrl();

        if(url!=null){ // se houver uma fto ele insere no circleImageView

            Glide.with(ConfiguracoesActivity.this)
                    .load( url )
                    .into(circleImageViewPerfil);

        }else{
            circleImageViewPerfil.setImageResource(R.drawable.padrao); // se n ele coloca a imagem padrao
        }

        editPerfilNome.setText( usuario.getDisplayName() ); // usuario, q faz referencia ao usuarioAtual, pega o nome e
                                                           // insere no campo de nome

        imgCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // intent para abrir um recurso do android (neste caso = câmera)
                Intent i = new Intent( MediaStore.ACTION_IMAGE_CAPTURE );

                if( i.resolveActivity( getPackageManager()) != null ) { // se é possivel abrir a camera:
                    startActivityForResult(i, SELECAO_CAMERA);
                }
            }
        });

        imgGaleria.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                // intent para abrir um recurso do android (neste caso: câmera)
                Intent i = new Intent( Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI ); // caminho padrao
                                                                                                    // para fotos no celular

                if( i.resolveActivity( getPackageManager()) != null ) { // se é possivel abrir a galeria:
                    startActivityForResult(i, SELECAO_GALERIA);
                }
            }
        });

        imgEdit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String nome = editPerfilNome.getText().toString();
                boolean retorno = UsuarioFirebase.atualizarNomeUsuario(nome);
                if(retorno){

                    usuarioLogado.setNome(nome);
                    usuarioLogado.atualizar();

                    Toast.makeText(getApplicationContext(), "Nome alterado com sucesso",
                            Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) { // data = dados retornados
        super.onActivityResult(requestCode, resultCode, data);

        if( resultCode == RESULT_OK ){ // entao o retorno dos dados deu certo

            Bitmap imagem = null;

            try {

                switch (requestCode){

                    case SELECAO_CAMERA:
                        imagem = (Bitmap) data.getExtras().get( "data" );
                        break;

                    case SELECAO_GALERIA:
                        Uri localImagemSelecionada = data.getData();
                        imagem = MediaStore.Images.Media.getBitmap( getContentResolver(), localImagemSelecionada );
                        break;
                }

                if(imagem != null){

                    circleImageViewPerfil.setImageBitmap( imagem );

                    // recuperar dados para firebase
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    imagem.compress(Bitmap.CompressFormat.JPEG,70, baos);
                    byte[] dadosImagem = baos.toByteArray();

                    // salvar dados firebase
                    final StorageReference imgRef = storageReference
                            .child("imagens")
                            .child("perfil")
                            .child( idUsuario + ".jpeg")
                            ;

                    UploadTask uploadTask = imgRef.putBytes( dadosImagem ); // salva a imagem no Storage do Firebase
                    uploadTask.addOnFailureListener(new OnFailureListener() { // tratamento para sucesso e falha
                        @Override
                        public void onFailure(@NonNull Exception e) {

                            Toast.makeText(getApplicationContext(), "Erro ao atualizar imagem de perfil",
                                    Toast.LENGTH_SHORT).show();

                        }
                    }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {

                            Toast.makeText(getApplicationContext(), "Imagem de perfil alterada",
                                    Toast.LENGTH_SHORT).show();

                            // atualizar imagem:
                            imgRef.getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
                                @Override
                                public void onComplete(@NonNull Task<Uri> task) {
                                    Uri url = task.getResult();
                                    atualizaFotoUsuario( url );
                                }
                            });

                        }
                    });
                }

            }catch (Exception e){
                e.printStackTrace();
            }

        }
    }

    public void atualizaFotoUsuario(Uri url){
        boolean retorno = UsuarioFirebase.atualizarFotoUsuario(url);
        if (retorno) {
            usuarioLogado.setFoto(url.toString());
            usuarioLogado.atualizar();

            Toast.makeText(getApplicationContext(),"Sua foto foi alterada",Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        for( int permissaoResultado: grantResults ){

            if( permissaoResultado == PackageManager.PERMISSION_DENIED){

                alertaValidacaoPermissao();
            }

        }
    }

    private void alertaValidacaoPermissao(){

        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle("Permissões negadas");
        alert.setMessage("Para utilizar o app é necessário aceitar as permissões");
        alert.setCancelable(false);
        alert.setPositiveButton("Confirmar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                finish();
            }
        });

        AlertDialog dialog = alert.create();
        dialog.show();
    }

}