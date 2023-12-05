import java.awt.GridLayout;
import java.awt.TextField;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;

import javax.swing.*;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.sun.xml.internal.ws.api.model.wsdl.WSDLOutput;

/**
 * This is a University Bookshop to support: (1) Add Order (2) Update Order
 * (3) Search Order (4) Delete Order
 * (by source, dest, stop_no = 0) (5) select a flight (by source, dest, stop_no
 * = 1)
 *
 *
 */

public class UniversityBookshop{

    Scanner in = null;
    Connection conn = null;
    // Database Host
    final String databaseHost = "orasrv1.comp.hkbu.edu.hk";
    // Database Port
    final int databasePort = 1521;
    // Database name
    final String database = "pdborcl.orasrv1.comp.hkbu.edu.hk";
    final String proxyHost = "faith.comp.hkbu.edu.hk";
    final int proxyPort = 22;
    final String forwardHost = "localhost";
    int forwardPort;
    Session proxySession = null;
    boolean noException = true;

    // JDBC connecting host
    String jdbcHost;
    // JDBC connecting port
    int jdbcPort;
    int currentYear = 2023;
    String[] options = { // if you want to add an option, append to the end of
            // this array
            "Add Order", "Update Order", "Search Order",
            "Delete Order",
            "exit" };

    /**
     * Get YES or NO. Do not change this function.
     *
     * @return boolean
     */
    boolean getYESorNO(String message) {
        JPanel panel = new JPanel();
        panel.add(new JLabel(message));
        JOptionPane pane = new JOptionPane(panel, JOptionPane.QUESTION_MESSAGE, JOptionPane.YES_NO_OPTION);
        JDialog dialog = pane.createDialog(null, "Question");
        dialog.setVisible(true);
        boolean result = JOptionPane.YES_OPTION == (int) pane.getValue();
        dialog.dispose();
        return result;
    }

    /**
     * Get username & password. Do not change this function.
     *
     * @return username & password
     */
    String[] getUsernamePassword(String title) {
        JPanel panel = new JPanel();
        final TextField usernameField = new TextField();
        final JPasswordField passwordField = new JPasswordField();
        panel.setLayout(new GridLayout(2, 2));
        panel.add(new JLabel("Username"));
        panel.add(usernameField);
        panel.add(new JLabel("Password"));
        panel.add(passwordField);
        JOptionPane pane = new JOptionPane(panel, JOptionPane.QUESTION_MESSAGE, JOptionPane.OK_CANCEL_OPTION) {
            private static final long serialVersionUID = 1L;

            @Override
            public void selectInitialValue() {
                usernameField.requestFocusInWindow();
            }
        };
        JDialog dialog = pane.createDialog(null, title);
        dialog.setVisible(true);
        dialog.dispose();
        return new String[] { usernameField.getText(), new String(passwordField.getPassword()) };
    }

    /**
     * Login the proxy. Do not change this function.
     *
     * @return boolean
     */
    public boolean loginProxy() {
        if (getYESorNO("Using ssh tunnel or not?")) { // if using ssh tunnel
            String[] namePwd = getUsernamePassword("Login cs lab computer");
            String sshUser = namePwd[0];
            String sshPwd = namePwd[1];
            try {
                proxySession = new JSch().getSession(sshUser, proxyHost, proxyPort);
                proxySession.setPassword(sshPwd);
                Properties config = new Properties();
                config.put("StrictHostKeyChecking", "no");
                proxySession.setConfig(config);
                proxySession.connect();
                proxySession.setPortForwardingL(forwardHost, 0, databaseHost, databasePort);
                forwardPort = Integer.parseInt(proxySession.getPortForwardingL()[0].split(":")[0]);
            } catch (JSchException e) {
                e.printStackTrace();
                return false;
            }
            jdbcHost = forwardHost;
            jdbcPort = forwardPort;
        } else {
            jdbcHost = databaseHost;
            jdbcPort = databasePort;
        }
        return true;
    }

    /**
     * Login the oracle system. Change this function under instruction.
     *
     * @return boolean
     */
    public boolean loginDB() {
        String username = "f2220143";//Replace e1234567 to your username
        String password = "f2220143";//Replace e1234567 to your password

        /* Do not change the code below */
        if(username.equalsIgnoreCase("e1234567") || password.equalsIgnoreCase("e1234567")) {
            String[] namePwd = getUsernamePassword("Login sqlplus");
            username = namePwd[0];
            password = namePwd[1];
        }
        String URL = "jdbc:oracle:thin:@" + jdbcHost + ":" + jdbcPort + "/" + database;

        try {
            System.out.println("Logging " + URL + " ...");
            conn = DriverManager.getConnection(URL, username, password);
            return true;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Show the options.
     */
    public void showOptions() {
        System.out.println("Please choose following option:");
        for (int i = 0; i < options.length; ++i) {
            System.out.println("(" + (i + 1) + ") " + options[i]);
        }
    }

    /**
     * Run the bookshop
     */
    public void run() {
        while (noException) {

            Date date = new Date();
            if(currentYear != date.getYear() + 1900)
            {
                resetDiscount(); // resets the discount to 0 at new year
                currentYear = date.getYear() + 1900;
            }
            showOptions();
            String line = in.nextLine();
            if (line.equalsIgnoreCase("exit"))
                return;
            int choice = -1;
            try {
                choice = Integer.parseInt(line);
            } catch (Exception e) {
                System.out.println("This option is not available");
                continue;
            }
            if (!(choice >= 1 && choice <= options.length)) {
                System.out.println("This option is not available");
                continue;
            }
            if (options[choice - 1].equals("Add Order")) {
                add();
            } else if (options[choice - 1].equals("Update Order")) {
               update();
            } else if (options[choice - 1].equals("Search Order")) {
                search();
            } else if (options[choice - 1].equals("Delete Order")) {
                delete();
            }
            else if (options[choice - 1].equals("exit")) {
                break;
            }
        }
    }

    /**
     * Close the University Bookshop. Do not change this function.
     */
    public void close() {
        System.out.println("Thanks for using this manager! Bye...");
        try {
            if (conn != null)
                conn.close();
            if (proxySession != null) {
                proxySession.disconnect();
            }
            in.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Constructor of University Bookshop Do not change this function.
     */
    public UniversityBookshop() {
        System.out.println("Welcome to use this bookshop!");
        in = new Scanner(System.in);
    }

    /**
     * resets the discount to 0
     */
    public void resetDiscount()
    {


        try {
            Statement stm = conn.createStatement();
            String sql = "UPDATE Total_Purchase SET purchased = 0";
            ResultSet rs = stm.executeQuery(sql);

            rs.close();
            stm.close();
        }
        catch (SQLException e){
            e.printStackTrace();
            noException=false;
        }
    }

    /**
     * To add an order
     */
    public void add(){
        try {
            String[] data = getData();
            // data[3] = [sid, paymentType, cardNo(null)]

            printStock(); // prints the books
            int oid = getOid(); //generates oid

            ArrayList<Integer> bidQuantity = getBidQuantity(); //gets the list of books

            String[] insertCommands = new String[bidQuantity.size() / 2];
            for (int i = 0; i < insertCommands.length; i++) {
                int bid = bidQuantity.get(2 * i);
                int quantity = bidQuantity.get(2*i +1);
                insertCommands[i] = "INSERT INTO ORDERED_BOOK VALUES (NULL, 0, " + bid + ", " + oid + ", " + quantity + ")";
            }//generate commands for books
            makeInitialOrder(data, oid); //insets to student_orders
            addBooksToOrder(insertCommands); //inserts to ordered_book
            double discount = getDiscount(data[0]); // gets the discount level
            double offset = (100.0000000 - discount) / 100; //calculates the offset
            applyDiscount(offset, data[0], oid); //applies discount
            updateTotalPurchase(data, oid); //updates total purchase
        }
        catch (Exception e){
            String[] mssg = e.getMessage().split("\n");
            System.out.println(mssg[0]);
        }
    }

    /**
     * INPUT: data[3] = [sid, paymentType, cardNo(null)]
     **/
    private String[] getData(){
        String[] data= new String[3];
        System.out.println("Enter the SID: ");
        Integer sid = in.nextInt();
        System.out.println("Enter the number for payment method: ");
        System.out.println("1 -----> CREDIT CARD");
        System.out.println("2 -----> CASH");
        System.out.println("3 -----> BANK TRANSFER");
        Integer i= in.nextInt();
        data[0]=sid.toString();
        if (i==1){
            System.out.println("Enter the card details XXXXXX");
            String cardDetail= in.next();
            data[1]="CREDITCARD";
            data[2]=cardDetail;
        }
        else if (i==2){
            data[1]="CASH";
            data[2]="NULL";
        }
        else {
            data[1]="BANKTRANSFER";
            data[2]="NULL";
        }
        return data;
    }

    /**
     * generates Oid
     */
    private int getOid()
    {

        try {
            Statement stm = conn.createStatement();
            String sql = "SELECT MAX(oid) FROM student_orders"; // gets the max oid
            ResultSet rs = stm.executeQuery(sql);
            if (!rs.next())
                return 1;
            int oid= rs.getInt(1)+1; // adds one to max oid
            rs.close();
            stm.close();

            return (oid);
        }
        catch (SQLException e){
            e.printStackTrace();
            noException=false;
        }
        return 1;
    }

    /**
     * makes and arraylist containing Bid and quantities of Bid
     */
    private ArrayList<Integer> getBidQuantity(){
        boolean stillOrdering = true;
        ArrayList<Integer> bid = new ArrayList<>();
        while (stillOrdering) {
            System.out.println("Enter Book BID: ");
            int bookid = in.nextInt();
            System.out.println("Enter Book Quantity: ");
            int quantity = in.nextInt();
            if (checkQuantity(bookid, quantity)) {
                bid.add(bookid); //even index bid
                bid.add(quantity); //odd index quantity
            } else {
                System.out.println("Book (" + bookid + ") !!! --> insufficient stock available");
            }
            in.nextLine();

            System.out.println("Add another book? (Y/N)");
            if (in.nextLine().equalsIgnoreCase("n"))
                stillOrdering = false;
        }
        return bid;
    }

    /**
     * checks if enough books are available
     */
    private boolean checkQuantity(int bid, int orderQuantity){
        int bookQuantity = 0;
        try {
            Statement stm = conn.createStatement();
            String sql = "SELECT amount FROM BOOK WHERE bid =" + bid;
            ResultSet rs = stm.executeQuery(sql);
            while (rs.next()) {
                bookQuantity = rs.getInt(1);
            }
            rs.close();
            stm.close();
        }
        catch (SQLException e){
            e.printStackTrace();
            noException=false;
        }

        return (orderQuantity <= bookQuantity);
    }

    /**
     * prints current stock of books
     */
    private void printStock(){;
        try {
            Statement stm = conn.createStatement();
            String sql = "SELECT * FROM BOOK";
            ResultSet rs = stm.executeQuery(sql);
            int resultCount = 0;
            while (rs.next()) {
                System.out.print(" "+ resultCount+ " ");
                for (int x=0; x<5; x++) {
                    System.out.printf("%12s  ",rs.getString(x+1));
                }
                System.out.println();
                ++resultCount;
            }
            rs.close();
            stm.close();
        }
        catch (SQLException e){
            e.printStackTrace();
            noException=false;
        }
    }

    /**
     * inserts into Student_Orders table
     */
    private void makeInitialOrder(String[] data, int oid) throws Exception{
        String sid = data[0];
        String paymentType = data[1];
        String cardNo = data[2];
        Statement stm = conn.createStatement();
            String sql = "INSERT INTO STUDENT_ORDERS VALUES (" + oid + ", " + sid + ", sysdate , 0, " + "'" + paymentType + "', ";
            if(data[2].equals("NULL"))
                sql+= cardNo + ")"; //INSERT INTO STUDENT_ORDERS VALUES (1, 2121, sysdate , 0, 'CASH', NULL)
            else
                sql+= "'" + cardNo + "')"; //INSERT INTO STUDENT_ORDERS VALUES (1, 2122, sysdate,  0, 'CREDITCARD', 'asd')


            ResultSet rs = stm.executeQuery(sql);
            rs.close();
            stm.close();

    }

    /**
     * inserts into ordered_book table;
     */
    private void addBooksToOrder(String[] insertCommands) throws Exception{
        for (String command : insertCommands) {
                Statement stm = conn.createStatement();
                String sql = command;
                ResultSet rs = stm.executeQuery(sql);
                rs.close();
                stm.close();
        }
    }

    /**
     * gets the discount level of a student
     */
    private double getDiscount(String sid){
        try {
            Statement stm = conn.createStatement();
            String sql = "SELECT discount_level FROM student where sid = " + sid ;
            ResultSet rs = stm.executeQuery(sql);
            if (!rs.next())
                return 0;
            double d= rs.getDouble(1);
            rs.close();
            stm.close();
            return d;
        }
        catch (SQLException e){
            e.printStackTrace();
            noException=false;
        }
        return -1.0f;
    }

    /**
     * applies discount levels
     */
    private void applyDiscount(double offset, String sid, int oid){
        try {
            Statement stm = conn.createStatement();
            String sql = " UPDATE STUDENT_ORDERS SET TOTAL_PRICE = TOTAL_PRICE * " + offset + "WHERE OID = " + oid + "";
            ResultSet rs = stm.executeQuery(sql);
            rs.close();
            stm.close();
        }
        catch (SQLException e){
            e.printStackTrace();
            noException=false;
        }
    }

    /**
     * updates the purchase history of a student
     */
    private void updateTotalPurchase(String[] data, int oid){
        try {
            Statement stm = conn.createStatement();
            String sql = "UPDATE TOTAL_PURCHASE SET PURCHASED = PURCHASED + (SELECT Total_price from STUDENT_ORDERS WHERE OID =" + oid +
                    ") WHERE sid = " + data[0];
            ResultSet rs = stm.executeQuery(sql);
            rs.close();
            stm.close();
        }
        catch (SQLException e){
            e.printStackTrace();
            noException=false;
        }
    }



    /**
     * update order
     */
    public void update()
    {
        int bid, oid;
        System.out.println("Enter the OID you want to update: ");
        oid = in.nextInt();
        System.out.println("Enter BID you want to update");
        bid = in.nextInt();
        try {
            Statement stm = conn.createStatement();
            String sql = " UPDATE ORDERED_BOOK SET delivered = 1 where OID =" + oid + "and BID = " + bid + "";
            ResultSet rs = stm.executeQuery(sql);
            sql = " UPDATE ORDERED_BOOK SET delivery_date = SYSDATE where OID =" + oid + "and BID = " + bid + "";
            rs = stm.executeQuery(sql);
            rs.close();
            stm.close();
        }
        catch (SQLException e){
            e.printStackTrace();
            noException=false;
        }

    }



    /**
     * search order
     */
    private void search(){
        String sid=getSearchSid();
        List<String> orders= oidList(sid);
        System.out.println(sid+" has following orders");
        System.out.println("--------------------------");
        for (String oid: orders){
            System.out.println();
            System.out.println("--------------------");
            System.out.println("| Order id  "+oid +"|");
            System.out.println("---------------------");
            displayOrder(oid);
        }
    }

    /**
     * inputs sid
     */
    private String getSearchSid(){
        Scanner sc= new Scanner(System.in);
        System.out.println("Please enter student's SID");
        return sc.nextLine();
    }

    /**
     * makes a list of OIDs of a student
     */
    public List<String> oidList(String sid){
        List<String> oids = new ArrayList<>();
        try {
            Statement stm = conn.createStatement();
            String sql = "SELECT oid FROM STUDENT_ORDERS WHERE SID =" + sid;
            ResultSet rs = stm.executeQuery(sql);
            while (rs.next()) {
                oids.add(rs.getInt(1)+"");
            }
            rs.close();
            stm.close();
        }
        catch (SQLException e){
            e.printStackTrace();
            noException=false;
        }
        return oids;
    }

    /**
     * displays one order
     */
    private void displayOrder(String oid){
        try {
            Statement stm = conn.createStatement();
            String sql = "SELECT bid, title, quantity, quantity*price, delivery_date FROM ORDERED_BOOK NATURAL JOIN BOOK WHERE OID=" + oid;
            ResultSet rs = stm.executeQuery(sql);
            System.out.printf("%12s  %12s  %12s  %12s  %12s\n","BID","TITLE","QUANTITY", "TOTAL_PRICE", "DELIVERY_DATE");
            while (rs.next()) {
                for (int x=0; x<5; x++) {
                    System.out.printf("%12s  ",rs.getString(x+1));
                }
                System.out.println();
            }
            rs.close();
            stm.close();
        }
        catch (SQLException e){
            e.printStackTrace();
            noException=false;
        }
    }



    /**
     * delete order
     */
    public void delete(){
        String sid = getSearchSid();
        List<String> orders = oidList(sid);
        String oid = getDeleteOid(orders);

        try {
            Statement stm = conn.createStatement();
            String sql = "DELETE FROM STUDENT_ORDERS WHERE oid = " + oid;
            ResultSet rs = stm.executeQuery(sql);
            rs.close();
            stm.close();
        }
        catch (SQLException e){
            String[] mssg = e.getMessage().split("\n");
            System.out.println("Deletion failed!\n" + mssg[0]);
        }

    }

    /**
     * inputs the oid to delete
     */
    private String getDeleteOid(List<String> orders){
        System.out.println("Here are your orders");
        for (String order : orders) {
            System.out.println("---> Order number " + order + ":");
            displayOrder(order);
        }

        Scanner sc= new Scanner(System.in);
        System.out.println("Enter the Order ID you would like to delete: ");
        return sc.nextLine();
    }


    /**
     * Main function
     *
     * @param args
     */
    public static void main(String[] args) {
        UniversityBookshop Bookshop = new UniversityBookshop();
        if (!Bookshop.loginProxy()) {
            System.out.println("Login proxy failed, please re-examine your username and password!");
            return;
        }
        if (!Bookshop.loginDB()) {
            System.out.println("Login database failed, please re-examine your username and password!");
            return;
        }
        System.out.println("Login succeed!");
        try {
            Bookshop.run();
        } finally {
            Bookshop.close();
        }
    }
}
