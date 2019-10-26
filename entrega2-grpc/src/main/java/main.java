import java.util.Scanner;

public class main {

    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);
        String state = "";

        do {
            System.out.println("state: " + state );
            System.out.println("Digita ae");
            String input = sc.nextLine();

            if (input == "sair") {
                    System.out.println("goooo");
            }
            state = input;
            System.out.println("state: " + state);
        } while (!state.equals("sair"));
    }
}
