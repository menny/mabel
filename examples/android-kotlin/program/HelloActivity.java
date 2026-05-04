package net.evendanan.bazel.mvn.example;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.collection.ArrayMap;

public class HelloActivity extends Activity {

  @Override
  protected void onCreate(@Nullable final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    ArrayMap<String, String> map = new ArrayMap<>();
    map.put("key", "value");

    setContentView(R.layout.main_activity);

    TextView t = findViewById(R.id.root_content);
    t.setText(new StringFormat().hello("mabel"));
  }
}
