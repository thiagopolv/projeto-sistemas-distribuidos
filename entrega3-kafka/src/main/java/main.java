import util.ConfigProperties;

public class main {

    public static void main(String[] args) {
        String initialHash = ConfigProperties.getProperties().getProperty("initial.hash");
        String finalHash = ConfigProperties.getProperties().getProperty("last.hash");
        System.out.println(initialHash);
        System.out.println(finalHash);

//        Scanner sc = new Scanner(System.in);
//        String state = "";
//
//        do {
//            System.out.println("state: " + state );
//            System.out.println("Digita ae");
//            String input = sc.nextLine();
//
//            if (input == "sair") {
//                    System.out.println("goooo");
//            }
//            state = input;
//            System.out.println("state: " + state);
//        } while (!state.equals("sair"));
    }
}
