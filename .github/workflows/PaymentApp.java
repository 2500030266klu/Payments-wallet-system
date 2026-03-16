package Payment;
import java.io.*;
import java.util.*;
class User {
    private static long accountCounter = 100000000000L;
    long accountNumber;
    long mobile;
    String name;
    long balance;

    public User(long mobile, String name, long balance) {
        this.accountNumber = accountCounter++;
        this.mobile = mobile;
        this.name = name;
        this.balance = balance;
    }

    public String toCSV() {
        return accountNumber + "," + mobile + "," + name + "," + balance;
    }

    // Expose counter so PaymentApp can keep it ahead of loaded account numbers
    public static void setCounterIfBehind(long accNo) {
        if (accNo >= accountCounter) {
            accountCounter = accNo + 1;
        }
    }

    @Override
    public String toString() {
        return "Account: " + accountNumber + " | " + name + " (" + mobile + ") - Balance: " + balance;
    }
}

class Transaction {
    String type;       // Deposit, Withdraw, Transfer
    String fromUser;
    String toUser;
    long amount;

    public Transaction(String type, String fromUser, String toUser, long amount) {
        this.type = type;
        this.fromUser = fromUser;
        this.toUser = toUser;
        this.amount = amount;
    }

    public String toCSV() {
        return type + "," + fromUser + "," + toUser + "," + amount;
    }
}

public class PaymentApp {
    private final List<User> users = new ArrayList<>();
    private static final String FILE_PATH = "Record.csv";

    private static final String TXN_FILE_PATH = "Transactions.csv";

    private static final String DELETED_FILE_PATH = "DeletedUsers.csv";

    private static final long REWARD_THRESHOLD = 10000; // ₹10,000 triggers a reward

    public void addUser(long mobile, String name, long balance) {
        for (User u : users) {
            if (u.mobile == mobile) {
                System.out.println("User already exists!");
                return;
            }
        }
        User newUser = new User(mobile, name, balance);
        users.add(newUser);
        System.out.println("User " + name + " added successfully.");
        appendUserToCSV(newUser);
    }

    public void deposit(String toInput, long amount) {
        try {
            User toUser = resolveUser(toInput);
            if (toUser == null) {
                System.out.println("Invalid user input.");
                return;
            }
            toUser.balance += amount;
            System.out.println(amount + " deposited to " + toUser.name);
            logTransaction(new Transaction("Deposit", "System", toUser.name, amount));
            saveAllUsersToCSV();

            if (amount >= REWARD_THRESHOLD) {
                rewardUser(toUser);
            }
        } catch (Exception e) {
            System.err.println("Error during deposit: " + e.getMessage());
        }
    }

    public void withdraw(String fromInput, long amount) {
        try {
            User fromUser = resolveUser(fromInput);
            if (fromUser == null) {
                System.out.println("Invalid user input.");
                return;
            }
            if (fromUser.balance >= amount) {
                fromUser.balance -= amount;
                System.out.println(amount + " withdrawn from " + fromUser.name);
                logTransaction(new Transaction("Withdraw", fromUser.name, "System", amount));
                saveAllUsersToCSV();

                if (amount >= REWARD_THRESHOLD) {
                    rewardUser(fromUser);
                }
            } else {
                System.out.println("Insufficient balance!");
            }
        } catch (Exception e) {
            System.err.println("Error during withdrawal: " + e.getMessage());
        }
    }

    public void transfer(String fromInput, String toInput, long amount) {
        try {
            User fromUser = resolveUser(fromInput);
            User toUser = resolveUser(toInput);

            if (fromUser == null || toUser == null) {
                System.out.println("Invalid users for transfer!");
                return;
            }

            if (fromUser.balance >= amount) {
                fromUser.balance -= amount;
                toUser.balance += amount;
                logTransaction(new Transaction("Transfer", fromUser.name, toUser.name, amount));
                saveAllUsersToCSV();
                System.out.println("Transferred " + amount + " from " + fromUser.name + " to " + toUser.name);

                if (amount >= REWARD_THRESHOLD) {
                    rewardUser(toUser);
                }
            } else {
                System.out.println("Insufficient balance!");
            }
        } catch (Exception e) {
            System.err.println("Error during transfer: " + e.getMessage());
        }
    }

    public void rewardUser(User u) {
        if (u == null) return;

        long rewardAmount = new Random().nextInt(100) + 1; // ₹1–₹100
        u.balance += rewardAmount;

        System.out.println("🎉 Reward of ₹" + rewardAmount + " credited to " + u.name + "'s account for high-value transaction!");

        saveAllUsersToCSV(); // persist updated balance to Record.csv

        try (FileWriter fw = new FileWriter("Rewards.csv", true)) {
            String timestamp = new Date().toString();
            fw.write(u.accountNumber + "," + u.name + "," + rewardAmount + "," + timestamp + "\n");
        } catch (IOException e) {
            System.out.println("Error logging reward: " + e.getMessage());
        }
    }

    public void checkBalance(String input) {
        try {
            User user = resolveUser(input);
            if (user != null) {
                System.out.println("Balance of " + user.name + ": " + user.balance);
            } else {
                System.out.println("User not found!");
            }
        } catch (Exception e) {
            System.err.println("Error checking balance: " + e.getMessage());
        }
    }

    public void deleteUser(String input) {
        try {
            User targetUser = resolveUser(input);
            if (targetUser != null) {
                logDeletedUser(targetUser);
                users.remove(targetUser);
                saveAllUsersToCSV();
                System.out.println("User " + targetUser.name + " deleted successfully.");
            } else {
                System.out.println("User not found!");
            }
        } catch (Exception e) {
            System.err.println("Error deleting user: " + e.getMessage());
        }
    }

    public void logDeletedUser(User user) {

        try (FileWriter writer = new FileWriter(DELETED_FILE_PATH, true)) {
            writer.append(user.accountNumber + ",");
            writer.append(user.mobile + ",");
            writer.append(user.name + ",");
            writer.append(user.balance + "\n");
        } catch (IOException e) {
            System.err.println("Error writing to DeletedUsers.csv: " + e.getMessage());
        }
    }

    public User resolveUser(String input) {
        if (input == null || input.trim().isEmpty()) {
            System.out.println("Error: Input cannot be null or empty.");
            return null;
        }

        input = input.trim();
        String target = input.toLowerCase();

        try {
            Long.parseLong(input);

            // Match mobile number first
            for (User u : users) {
                if (u != null && String.valueOf(u.mobile).equals(input)) {
                    return u;
                }
            }

            // Then match account number
            for (User u : users) {
                if (u != null && String.valueOf(u.accountNumber).equals(input)) {
                    return u;
                }
            }
        } catch (NumberFormatException e) {
            // Input is not a number — try name match
            for (User u : users) {
                if (u != null && u.name != null && u.name.trim().toLowerCase().equals(target)) {
                    return u;
                }
            }
        }

        System.out.println("User not found for input: " + input);
        return null;
    }

    public void showAllUsers() {
        if (users.isEmpty()) {
            System.out.println("No users available.");
        } else {
            System.out.println("\n--- All Users ---");
            for (User user : users) {
                System.out.println(user);
            }
        }
    }

    public static void showAllTransactions() {
        try (BufferedReader br = new BufferedReader(new FileReader(TXN_FILE_PATH))) {
            String line;
            System.out.println("---- All Transactions ----");
            while ((line = br.readLine()) != null) {
                System.out.println(line);
            }
        } catch (IOException e) {
            System.out.println("Error reading transactions file: " + e.getMessage());
        }
    }

    public static void showDeletedUsers() {
        try (BufferedReader br = new BufferedReader(new FileReader(DELETED_FILE_PATH))) {
            String line;
            System.out.println("---- Deleted Users ----");
            while ((line = br.readLine()) != null) {
                System.out.println(line);
            }
        } catch (IOException e) {
            System.out.println("Error reading deleted users file: " + e.getMessage());
        }
    }

    private void appendUserToCSV(User user) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(FILE_PATH, true))) {
            bw.write(user.toCSV());
            bw.newLine();
        } catch (IOException e) {
            System.out.println("Error writing CSV: " + e.getMessage());
        }
    }

    public void saveAllUsersToCSV() {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(FILE_PATH))) {
            for (User user : users) {
                bw.write(user.toCSV());
                bw.newLine();
            }
        } catch (IOException e) {
            System.out.println("Error writing CSV: " + e.getMessage());
        }
    }

    private void logTransaction(Transaction txn) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(TXN_FILE_PATH, true))) {
            bw.write(txn.toCSV());
            bw.newLine();
        } catch (IOException e) {
            System.out.println("Error writing transaction CSV: " + e.getMessage());
        }
    }

    public void loadUsersFromCSV() {
        try (BufferedReader br = new BufferedReader(new FileReader(FILE_PATH))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 4) {
                    try {
                        long accNo   = Long.parseLong(parts[0].trim());
                        long mobile  = Long.parseLong(parts[1].trim());
                        String name  = parts[2].trim();

                        long balance = Long.parseLong(parts[3].trim());

                        User u = new User(mobile, name, balance);
                        u.accountNumber = accNo;
                        users.add(u);

                        User.setCounterIfBehind(accNo);
                    } catch (NumberFormatException e) {
                        System.out.println("Skipping invalid line: " + line);
                    }
                }
            }
        } catch (FileNotFoundException e) {
            System.out.println("No previous records found. Starting fresh.");
        } catch (IOException e) {
            System.out.println("Error reading CSV: " + e.getMessage());
        }
    }

    static void main(String[] args) {
        PaymentApp system = new PaymentApp();
        Scanner sc = new Scanner(System.in);

        system.loadUsersFromCSV();

        int choice = 0;
        do {
            System.out.println("\n--- PaymentApp Menu ---");
            System.out.println("1.  Add User");
            System.out.println("2.  Deposit");
            System.out.println("3.  Withdraw");
            System.out.println("4.  Transfer");
            System.out.println("5.  Delete User");
            System.out.println("6.  Check Balance");
            System.out.println("7.  Show All Users");
            System.out.println("8.  Show All Transaction History");
            System.out.println("9.  Show All Deleted Users");
            System.out.println("10. Exit (Save All)");
            System.out.print("Choice: ");

            try {
                choice = sc.nextInt();
                sc.nextLine(); // consume newline

                switch (choice) {
                    case 1:
                        System.out.print("Mobile: ");
                        long m = sc.nextLong(); sc.nextLine();
                        System.out.print("Name: ");
                        String n = sc.nextLine();
                        System.out.print("Balance: ");
                        // BUG 6 FIX: read balance as long
                        long b = sc.nextLong(); sc.nextLine();
                        system.addUser(m, n, b);
                        break;

                    case 2:
                        System.out.print("Deposit to (Mobile/Name/AccNo): ");
                        String depositInput = sc.nextLine();
                        // BUG 5 FIX: resolve once, pass string (method handles null internally)
                        System.out.print("Amount: ");
                        long da = sc.nextLong(); sc.nextLine();
                        system.deposit(depositInput, da);
                        break;

                    case 3:
                        System.out.print("Withdraw from (Mobile/Name/AccNo): ");
                        String withdrawInput = sc.nextLine();
                        System.out.print("Amount: ");
                        long wa = sc.nextLong(); sc.nextLine();
                        system.withdraw(withdrawInput, wa);
                        break;

                    case 4:
                        System.out.print("From (Mobile/Name/AccNo): ");
                        String fromInput = sc.nextLine();
                        System.out.print("To (Mobile/Name/AccNo): ");
                        String toInput = sc.nextLine();
                        System.out.print("Amount: ");
                        long ta = sc.nextLong(); sc.nextLine();
                        system.transfer(fromInput, toInput, ta);
                        break;

                    case 5:
                        System.out.print("Delete user (Mobile/Name/AccNo): ");
                        String delInput = sc.nextLine();
                        system.deleteUser(delInput);
                        break;

                    case 6:
                        System.out.print("Check balance for (Mobile/Name/AccNo): ");
                        String balInput = sc.nextLine();
                        system.checkBalance(balInput);
                        break;

                    case 7:
                        system.showAllUsers();
                        break;

                    case 8:
                        showAllTransactions();
                        break;

                    case 9:
                        showDeletedUsers();
                        break;

                    case 10:
                        system.saveAllUsersToCSV();
                        System.out.println("Exiting. Data Saved.");
                        break;

                    default:
                        System.out.println("Invalid choice. Please enter a number between 1 and 10.");
                }
            } catch (InputMismatchException e) {
                System.out.println("Invalid input! Please enter a number for your choice.");
                sc.nextLine(); // clear invalid input
            }
        } while (choice != 10);

        sc.close();
    }
}
