package mr.lmd.personal.xysimageop2;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;


public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void btnMesh(View view) {
        startActivity(new Intent(this, MeshViewTest.class));
    }

    public void btnReflect(View view) {
        startActivity(new Intent(this, ReflectViewTest.class));
    }

    public void btnShader(View view) {
        startActivity(new Intent(this, BitmapShaderTest.class));
    }

    public void btnXfermode(View view) {
        startActivity(new Intent(this, RoundRectXfermodTest.class));
    }

    public void btnMatrix(View view) {
        startActivity(new Intent(this, ImageMatrixTest.class));
    }
}
