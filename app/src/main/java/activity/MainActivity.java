package activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.viewpager.widget.ViewPager;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import fragment.ContatosFragment;
import fragment.ConversasFragment;
import com.example.whatsapp.R;
import com.google.firebase.auth.FirebaseAuth;
import com.miguelcatalan.materialsearchview.MaterialSearchView;
import com.ogaclejapan.smarttablayout.SmartTabLayout;
import com.ogaclejapan.smarttablayout.utils.v4.FragmentPagerItemAdapter;
import com.ogaclejapan.smarttablayout.utils.v4.FragmentPagerItems;

import config.ConfiguracaoFirebase;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth autenticacao;
    private MaterialSearchView searchView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        autenticacao = ConfiguracaoFirebase.getFirebaseAutenticacao();

        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("WhatsApp");
        setSupportActionBar( toolbar ); // p ter suporte a versoes anteriores do android

        FragmentPagerItemAdapter adapter = new FragmentPagerItemAdapter(
              getSupportFragmentManager(),
                FragmentPagerItems.with( this)
                        .add( "Conversas" , ConversasFragment.class )
                        .add( "Contatos" , ContatosFragment.class )
                        .create()
        );

        ViewPager viewPager = findViewById(R.id.viewPager);
        viewPager.setAdapter( adapter );

        SmartTabLayout smartTabLayout = findViewById(R.id.viewPagerTab);
        smartTabLayout.setViewPager( viewPager );

        // conf search view
        searchView = findViewById(R.id.materialSearchPrincipal);

        searchView.setOnSearchViewListener(new MaterialSearchView.SearchViewListener() {
            @Override
            public void onSearchViewShown() {

            }

            @Override
            public void onSearchViewClosed() {
                ConversasFragment fragment =  (ConversasFragment) adapter.getPage( 0 );
                fragment.recarregarConversas();
            }
        });

        searchView.setOnQueryTextListener(new MaterialSearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {

                // verifica se esta pesquisando conversas ou contato

                switch ( viewPager.getCurrentItem() ){

                    case 0:
                        ConversasFragment conversasFragment = (ConversasFragment) adapter.getPage( 0 ); // 1°fragmento

                        if( newText != null && !newText.isEmpty() ){
                            conversasFragment.pesquisarConversas( newText );
                        }else {
                            conversasFragment.recarregarConversas();
                        }

                        break;

                    case 1:

                        ContatosFragment contatosFragment = (ContatosFragment) adapter.getPage( 1 );

                        if( newText != null && !newText.isEmpty() ){
                            contatosFragment.pesquisarContatos( newText );
                        }else {
                            contatosFragment.recarregarContatos();
                        }
                        break;

                }

                return false;
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater = getMenuInflater();
        inflater.inflate( R.menu.menu_main, menu );

        // configurar pesquisar para abrir o SearchView

        MenuItem item = menu.findItem( R.id.menuPesquisa ); // id do menu_main
        searchView.setMenuItem( item );

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        switch (item.getItemId()){

            case R.id.menu_sair:
                deslogarUsuario();
                finish();
                break;

            case R.id.menu_conf:
                abrirConf();
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    public void deslogarUsuario(){

        try{
            autenticacao.signOut();
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    public void abrirConf(){

        Intent intent = new Intent(MainActivity.this, ConfiguracoesActivity.class);
        startActivity( intent );

    }

}