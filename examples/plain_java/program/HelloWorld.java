package examples.plain_java.program;

import com.google.common.base.Joiner;

public class HelloWorld {
    public static void main(String[] args) throws Exception {
        System.out.println(
                Joiner.on(" ").join("Hello", "World!")
        );
    }
}
