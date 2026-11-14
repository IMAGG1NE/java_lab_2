import command.Command;
import command.SqlParser;
import engine.DbEngine;
import engine.FileDbEngine;
import model.ResultQ;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        DbEngine engine = new FileDbEngine();
        Scanner scanner = new Scanner(System.in);

        System.out.println("СУБД запущена. Введите SQL-запрос или exit для выхода.");

        while (true) {
            System.out.print("sql> ");
            String input = scanner.nextLine().trim();

            if (input.equalsIgnoreCase("exit")) {
                System.out.println("Завершение работы ");
                break;
            }
            if (input.isEmpty()) {
                continue;
            }
            Command cmd = SqlParser.parse(input);
            ResultQ result = cmd.execute(engine);
            TablePrinter.print(result);
        }
        scanner.close();
    }
}