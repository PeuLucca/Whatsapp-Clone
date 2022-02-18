package helper;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

public class Permissao {

    public static boolean validarPermissoes( String[] permissoes, Activity activity, int requestCode ){
        // aqui ele recebe permissoes, que são as permissoes que o app vai precisar

        if( Build.VERSION.SDK_INT >=23 ){ // obs: versão marshmallow, versoes anteriores nao precisa de validacao

            List<String> listaPermissoes = new ArrayList<>();
            for( String permissao : permissoes ){
                Boolean temPermissao =  ContextCompat.checkSelfPermission( activity, permissao )
                        == PackageManager.PERMISSION_GRANTED; // aqui verifica se o app já tem essas permissoes
                                                             // concedidas ou não

                if( !temPermissao ) listaPermissoes.add(permissao); // caso nao tenha permissao ele adiciona na list
            }

            if( listaPermissoes.isEmpty() ) return true; // caso a list for vazia (permissao ja concedida)
                                                        // entao finaliza

            String[] novasPermissoes = new String[ listaPermissoes.size() ]; // passa as permissoes necessarias para
            listaPermissoes.toArray( novasPermissoes );                     // este array de String

            ActivityCompat.requestPermissions( activity, novasPermissoes, requestCode ); // finalmente faz o request
                                                                                        // para as permissoes
        }

        return true;
    }

}
