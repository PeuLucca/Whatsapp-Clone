package activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;

import com.bumptech.glide.Glide;
import com.example.whatsapp.databinding.ActivityChatBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.provider.MediaStore;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.navigation.ui.AppBarConfiguration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.whatsapp.R;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import adapter.MensagensAdapter;
import config.ConfiguracaoFirebase;
import de.hdodenhof.circleimageview.CircleImageView;
import helper.Base64Custom;
import helper.UsuarioFirebase;
import model.Conversa;
import model.Grupo;
import model.Mensagem;
import model.Usuario;

public class ChatActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private ActivityChatBinding binding;
    private TextView textViewNome;
    private ImageView imageCamera;
    private CircleImageView circleImageViewFoto;
    private Usuario usuarioDestinatario;
    private Grupo grupo;
    private EditText editMensagem;
    private RecyclerView recyclerMensagens;
    private MensagensAdapter adapter;
    private List<Mensagem> mensagens = new ArrayList<>();
    private DatabaseReference database;
    private StorageReference storage;
    private DatabaseReference mensagensRef;
    private ChildEventListener childEventListenerMensagens;
    private Usuario usuarioRemetente;

    private static final int SELECAO_CAMERA = 100;

    // identificador usuarios remetentes e destinatario
    private String idUsuarioRemetente;
    private String idUsuarioDestinatario;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled( true ); // icone voltar
        getSupportActionBar().setDisplayShowTitleEnabled(false);

        textViewNome = findViewById(R.id.textViewNomeChat);
        circleImageViewFoto = findViewById(R.id.circleImageFoto);
        editMensagem = findViewById(R.id.editMensagem);
        recyclerMensagens = findViewById(R.id.recyclerMensagens);
        imageCamera = findViewById(R.id.imageCamera);

        // recupera dados do usuario remetente
        idUsuarioRemetente = UsuarioFirebase.getIdentificadorUsuario();
        usuarioRemetente = UsuarioFirebase.getDadosUsuarioLogado();

        Bundle bundle = getIntent().getExtras();
        if( bundle != null ){

            if( bundle.containsKey("chatGrupo") ){

                grupo = (Grupo) bundle.getSerializable("chatGrupo");
                idUsuarioDestinatario = grupo.getId();

                String foto = grupo.getFoto();
                if( foto != null ){
                    Uri url = Uri.parse( foto );
                    Glide.with( ChatActivity.this )
                            .load( url )
                            .into( circleImageViewFoto );
                }else {
                    circleImageViewFoto.setImageResource(R.drawable.padrao);
                }

                textViewNome.setText( grupo.getNome() );

            }else {

                usuarioDestinatario =  (Usuario) bundle.getSerializable( "chatContato" );
                textViewNome.setText( usuarioDestinatario.getNome() );
                String foto = usuarioDestinatario.getFoto();
                if( foto != null ){
                    Uri url = Uri.parse( foto );
                    Glide.with( ChatActivity.this )
                            .load( url )
                            .into( circleImageViewFoto );
                }else {
                    circleImageViewFoto.setImageResource(R.drawable.padrao);
                }

                // recuperar dados do usuario destinatario
                idUsuarioDestinatario = Base64Custom.codificarBase64( usuarioDestinatario.getEmail() );
            }
        }

        // configurar adapter
        adapter = new MensagensAdapter( mensagens , getApplicationContext() );

        // configurar recyclerView
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getApplicationContext());
        recyclerMensagens.setLayoutManager( layoutManager );
        recyclerMensagens.setHasFixedSize( true );
        recyclerMensagens.setAdapter( adapter );

        database = ConfiguracaoFirebase.getFirebaseDatabase();
        storage = ConfiguracaoFirebase.getFirebaseStorage();
        mensagensRef = database.child( "mensagens" )
                .child(idUsuarioRemetente)
                .child(idUsuarioDestinatario);

        // configurar camera
        imageCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent i = new Intent( MediaStore.ACTION_IMAGE_CAPTURE );

                if( i.resolveActivity( getPackageManager()) != null ) { // se é possivel abrir a camera:
                    startActivityForResult(i, SELECAO_CAMERA);
                }
            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if( resultCode == RESULT_OK ){

            Bitmap imagem = null;
            try {
                switch (requestCode){

                    case SELECAO_CAMERA:
                        imagem = (Bitmap) data.getExtras().get( "data" );
                        break;
                }

                if(imagem != null){
                    // recuperar dados para firebase
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    imagem.compress(Bitmap.CompressFormat.JPEG,70, baos);
                    byte[] dadosImagem = baos.toByteArray();

                    String nomeImagem = UUID.randomUUID().toString();

                    // configurar as referencias do firebase
                    StorageReference imgRef = storage.child("imagens")
                            .child("fotos")
                            .child(idUsuarioRemetente)
                            .child(nomeImagem);

                    UploadTask uploadTask = imgRef.putBytes( dadosImagem );
                    uploadTask.addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Toast.makeText(getApplicationContext(), "Erro ao fazer o upload",Toast.LENGTH_SHORT).show();
                        }
                    }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            imgRef.getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
                                @Override
                                public void onComplete(@NonNull Task<Uri> task) {
                                    Uri url = task.getResult(); // recupera url da imagem

                                    if( usuarioDestinatario != null ){ // mensagem normal

                                        Mensagem mensagem = new Mensagem();
                                        mensagem.setIdUsuario( idUsuarioRemetente );
                                        mensagem.setMensagem( "imagem.jpeg" );
                                        mensagem.setImagem( url.toString() );

                                        // salvar mensagem para remetente
                                        salvarMensagem( idUsuarioRemetente, idUsuarioDestinatario, mensagem );

                                        // salvar mensagem para destinatario
                                        salvarMensagem( idUsuarioDestinatario, idUsuarioRemetente, mensagem );

                                    }else { // mensagem em grupo

                                        for( Usuario membro: grupo.getMembros() ){

                                            String idRemetenteGrupo = Base64Custom.codificarBase64( membro.getEmail() );
                                            String idUsuarioLogadoGrupo  = UsuarioFirebase.getIdentificadorUsuario();

                                            Mensagem mensagem = new Mensagem();
                                            mensagem.setNome( usuarioRemetente.getNome() );
                                            mensagem.setIdUsuario( idUsuarioLogadoGrupo  );
                                            mensagem.setMensagem( "imagem.jpeg" );
                                            mensagem.setImagem( url.toString() );

                                            salvarMensagem( idRemetenteGrupo, idUsuarioDestinatario, mensagem );

                                            salvarConversa( idRemetenteGrupo, idUsuarioDestinatario, usuarioDestinatario ,
                                                    mensagem , true );
                                        }

                                    }

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

    public void enviarMensagem (View view){
        String textoMensagem = editMensagem.getText().toString();

        if( !textoMensagem.isEmpty() ){

            if( usuarioDestinatario != null ){ // mensagem normal

                Mensagem mensagem = new Mensagem();
                mensagem.setIdUsuario( idUsuarioRemetente );
                mensagem.setMensagem( textoMensagem );

                // salvar mensagem para remetente
                salvarMensagem( idUsuarioRemetente, idUsuarioDestinatario, mensagem );

                // salvar mensagem para destinatario
                salvarMensagem( idUsuarioDestinatario, idUsuarioRemetente, mensagem );

                // salvar a conversa para remetente
                salvarConversa( idUsuarioRemetente, idUsuarioDestinatario, usuarioDestinatario,
                        mensagem, false);

                // salvar a conversa para destinatario
                salvarConversa( idUsuarioDestinatario , idUsuarioRemetente, usuarioRemetente,
                        mensagem, false);

            }else{ // mensagem grupo

                for( Usuario membro: grupo.getMembros() ){

                    String idRemetenteGrupo = Base64Custom.codificarBase64( membro.getEmail() );
                    String idUsuarioLogadoGrupo  = UsuarioFirebase.getIdentificadorUsuario();

                    Mensagem mensagem = new Mensagem();
                    mensagem.setNome( usuarioRemetente.getNome() );
                    mensagem.setIdUsuario( idUsuarioLogadoGrupo  );
                    mensagem.setMensagem( textoMensagem );

                    salvarMensagem( idRemetenteGrupo, idUsuarioDestinatario, mensagem );

                    salvarConversa( idRemetenteGrupo, idUsuarioDestinatario, usuarioDestinatario ,
                            mensagem , true );
                }

            }
        }
    }

    private void salvarMensagem( String idRemetente, String idDestinatario, Mensagem mensagem ){

        DatabaseReference database = ConfiguracaoFirebase.getFirebaseDatabase();
        DatabaseReference mensagemRef = database.child("mensagens");

        mensagemRef.child( idRemetente )
                .child( idDestinatario )
                .push() // id da mensagem que o firebase cria automaticamente
                .setValue( mensagem );

        editMensagem.setText("");

    }

    private void salvarConversa( String idRemetente, String idDestinatario, Usuario usuarioExibicao,
                                 Mensagem msg, Boolean isGroup){

        Conversa conversaRemetente = new Conversa();
        conversaRemetente.setIdRemetente( idRemetente );
        conversaRemetente.setIdDestinatario( idDestinatario );
        conversaRemetente.setUltimaMensagem( msg.getMensagem() );

        if( isGroup ){

            conversaRemetente.setGrupo( grupo );

        }else {
            conversaRemetente.setUsuarioExibicao( usuarioExibicao );
        }

        conversaRemetente.salvar();
    }

    private void recuperarMensagens(){

        childEventListenerMensagens = mensagensRef.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                Mensagem mensagem = snapshot.getValue( Mensagem.class );
                mensagens.add( mensagem );

                adapter.notifyDataSetChanged();
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {

            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();
        recuperarMensagens();
    }

    @Override
    protected void onStop() {
        super.onStop();
        mensagensRef.removeEventListener(childEventListenerMensagens);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        /*NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_chat);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();*/
        return true;
    }
}