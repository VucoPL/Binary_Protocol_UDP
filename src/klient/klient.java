//@author Michal Najborowski & Maciej Anglart

package klient;
import java.io.*;
import java.net.*;
import java.text.DecimalFormat;
import java.util.*;

public class klient
{
    static wiadomosc txt = new wiadomosc();
    private static InetAddress host;
    private static final int PORT = 1111;

    private static DatagramSocket datagramSocket;
    private static DatagramPacket inPacket,outPacket;

    public static byte[] buffer_in = new byte[2];
    public static byte[] buffer_out = new byte[2];
    static public byte[] packet = new byte[2];
    static int packet_size = 2;
    public static boolean no_entry = false;

    public static int attempts;

    static String oper = "0000";
    static String odp = "000";
    static String id = "0000";
    static String liczba = "00000";

    static BitSet packet_bitset = new BitSet(16);

    public static void main(String[] args)
    {
        try
        {
            byte[] ipAddr = new byte[] {(byte)169, (byte)254, (byte)0, (byte)51};
            host = InetAddress.getByAddress(ipAddr);
        }
        catch(UnknownHostException uhEx)
        {
            System.out.println("Nie znaleziono ID hosta... ");
            System.exit(1);
        }

        serwer();
    }

    private static void serwer()
    {
        DecimalFormat df5 = new DecimalFormat("00000");
        try
        {
            datagramSocket = new DatagramSocket();
            Scanner scanner = new Scanner(System.in);
            int number;

            inPacket = new DatagramPacket(buffer_in,packet_size);
            outPacket = new DatagramPacket(buffer_out, packet_size, host, PORT);

            oper = txt.POWITANIE; //0001
            odp = txt.OK; //110
            id = txt.FULL; //0000
            liczba = txt.NUM; // 00000

            //WYSLANIE POWITALNEGO PAKIETU
            pack_in();
            datagramSocket.send(outPacket);

            //OTRZYMANIE PAKIETU Z ID
            reset_packet();
            datagramSocket.receive(inPacket);
            unpack();

            if(id.equals("0000"))
            {
                System.out.println("MAX 2 KLIENTOW");
                datagramSocket.close();
                System.exit(0);
            }

            System.out.println("Twoje ID: " + id);

            number = 1;
            while((number % 2 == 1) || (number > 31) || (number < 1))
            {
                System.out.print("\nPodaj dowolna liczbe parzysta L (od 2 do 30): ");
                number = scanner.nextInt();
            }

            buffer_in = new byte[2];
            buffer_out = new byte[2];
            packet = new byte[2];

            outPacket = new DatagramPacket(buffer_out, packet_size, host, PORT);

            oper = txt.LICZBA;
            odp = txt.OK;
            liczba = Integer.toBinaryString(number);
            liczba = df5.format(Integer.parseInt(liczba));

            //WYSLANIE PAKIETU Z LICZBA
            pack_in();
            datagramSocket.send(outPacket);

            //OTRZYMANIE PAKIETU Z LICZBA PROB
            inPacket = new DatagramPacket(buffer_in, packet_size);
            datagramSocket.receive(inPacket);
            unpack();

            attempts = bits_to_int(liczba);
            System.out.println("\n\nLiczba prob wynosi: " + attempts);


            //ZGADYWANIE LICZBY TAJNEJ
            for (int i=0; i<attempts; i++)
            {
                number = -1;
                while((number > 31) || (number < 0))
                {
                    System.out.print("Podaj liczbe (od 0 do 31): ");
                    number = scanner.nextInt();
                }

                //WYSLANIE PAKIETU Z LICZBA
                liczba = Integer.toBinaryString(number);
                liczba = df5.format(Integer.parseInt(liczba));
                pack_in();
                outPacket = new DatagramPacket(buffer_out, packet_size, host, PORT);
                datagramSocket.send(outPacket);

                //ODEBRANIE PAKIETU Z WYNIKIEM ZGADYWANIA
                reset_packet();
                inPacket = new DatagramPacket(buffer_in, packet_size);
                datagramSocket.receive(inPacket);
                unpack();

                //WYGRANA
                if (oper.equals(txt.WYNIK) && odp.equals(txt.WYGR)) {
                    System.out.println("\nGratulacje! Wygrales.");
                    break;
                }

                //PRZECIWNIK WYGRAL
                else if (oper.equals(txt.WYNIK) && odp.equals(txt.PRZEGR)) {
                    System.out.println("\nNiestety przeciwnik byl lepszy. Przegrales 😞");
                    break;
                }

                //BLEDNA ODPOWIEDZ
                else if (oper.equals(txt.WYNIK) && odp.equals(txt.BLAD)) {

                    if(i == attempts - 1)
                    {
                        System.out.println("\nWykorzystales wszystkie proby");
                    }
                    else{
                        System.out.println("\nPudlo! Probuj dalej.");
                    }
                }
            }
        }
        catch(IOException ioEx){
            ioEx.printStackTrace();
        }
        finally
        {
            System.out.println("\nZamykanie polaczenia... ");
            datagramSocket.close();
        }
    }

    public static void pack_in()
    {
        String byte1 = oper + odp + id.substring(0,1);
        int byte_int1 = bits_to_int(byte1);
        buffer_out[0] = (byte)byte_int1;

        String byte2 = id.substring(1, 4) + liczba;
        int byte_int2 = bits_to_int(byte2);
        buffer_out[1] = (byte) byte_int2;
    }

    public static void reset_packet()
    {
        oper = "0000";
        odp = "000";
        id = "0000";
        liczba = "00000";
    }

    public static void unpack()
    {
        char[] bits = new char[16];
        packet_bitset = fromByteArray(buffer_in);

        for(int i = 0; i < 16; i++)
        {
            if(packet_bitset.get(i) == true)
            {
                bits[i] = '1';
            }
            else if(packet_bitset.get(i) == false)
            {
                bits[i] = '0';
            }
        }

        String twoBytes = new String(bits);

        oper = twoBytes.substring(12, 16);
        String oper_str = new StringBuffer(oper).reverse().toString();
        oper = oper_str;

        odp = twoBytes.substring(9, 12);
        String odp_str = new StringBuffer(odp).reverse().toString();
        odp = odp_str;

        id = twoBytes.substring(5, 9);
        String id_str = new StringBuffer(id).reverse().toString();
        id = id_str;

        liczba = twoBytes.substring(0, 5);
        String num_str = new StringBuffer(liczba).reverse().toString();
        liczba = num_str;
    }

    public static void display_packet()
    {
        System.out.println(oper + " " + odp + " " + id + " " + liczba);
    }

    public static int bits_to_int (String s)
    {
        int x = 0;
        for(int i = s.length() - 1, p = 1; i>=0; i--, p*=2)
        {
            if(s.charAt(i) == '1')
            {
                x += p;
            }
        }
        return x;
    }

    public static byte[] toByteArray(BitSet bits)
    {
        byte[] bytes = new byte[(bits.length() + 7) / 8];
        for (int i=0; i<bits.length(); i++) {
            if (bits.get(i)) {
                bytes[bytes.length-i/8-1] |= 1<<(i%8);
            }
        }
        return bytes;
    }

    public static BitSet fromByteArray(byte[] bytes)
    {
        BitSet bits = new BitSet();
        for (int i = 0; i < bytes.length * 8; i++) {
            if ((bytes[bytes.length - i / 8 - 1] & (1 << (i % 8))) > 0) {
                bits.set(i);
            }
        }
        return bits;
    }
}