package edu.uoc.resolvers;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.net.MailTo;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/*
    Esta clase representa la vista de la página de ayuda.
 */
public class ActividadAyuda extends AppCompatActivity {
    private WebView vistaAyuda;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.actividad_ayuda);

        vistaAyuda = findViewById(R.id.actividadAyuda);
        vistaAyuda.setWebViewClient(new WebViewClient());
        vistaAyuda.loadUrl(getString(R.string.url_ayuda));

        vistaAyuda.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url){
                if(url.startsWith("mailto:")){
                    MailTo mt = MailTo.parse(url);
                    Intent i = new Intent(Intent.ACTION_SEND);
                    i.setType("text/plain");
                    i.putExtra(Intent.EXTRA_EMAIL, new String[]{mt.getTo()});
                    startActivity(i);
                    view.reload();
                }
                return true;
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (vistaAyuda.canGoBack()){
            vistaAyuda.goBack();
        }else {
            super.onBackPressed();
        }
    }

}
