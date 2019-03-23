package serwer;

import java.net.*;
//@author Michal Najborowski & Maciej Anglart

import java.text.DecimalFormat;
import java.util.BitSet;
import java.util.Random;

public class serwer {

    private static final int PORT = 1111;
    private static DatagramSocket datagramSocket;
    private static DatagramPacket inPacket, outPacket;

    static public byte[] packet = new byte[2];
    public static byte[] buffer_in = new byte[2];
    public static byte[] buffer_out = new byte[2];
    static int packet_size = 2;
    public static boolean if_end = false;

    static String id1;
    static String id2;

    static int rand_id1, rand_id2;
    static int secret_number;
    static int attempts = 0;

    static String oper = "0000";
    static String odp = "000";
    static String id = "0000";
    static String liczba = "00000";
    static BitSet packet_bitset = new BitSet(16);

    public static int counter = 0;

    public static void main(String [] args)
    {
        pack_in();

        System.out.println("Oczekiwanie na graczy... ");

        try
        {
            datagramSocket = new DatagramSocket(PORT);
        }catch(SocketException e)
        {
            System.out.println("Nie mozna skorzystac z tego portu.");
            System.exit(1);
        }
        client();
    }

    public static void client()
    {
        DecimalFormat df4 = new DecimalFormat("0000");
        DecimalFormat df5 = new DecimalFormat("00000");
        Random generator = new Random();
        rand_id1 = generator.nextInt(14) + 1;

        do {
            rand_id2 = generator.nextInt(14) + 1;
        } while(rand_id2 == rand_id1);

        id1 = Integer.toBinaryString(rand_id1);
        id1 = df4.format(Integer.parseInt(id1));

        id2 = Integer.toBinaryString(rand_id2);
        id2 = df4.format(Integer.parseInt(id2));

        int clientPort;
        int clientPort1 = 0;

        try {
            InetAddress clientAddress = null;
            InetAddress clientAddress1 = null;

            wiadomosc txt = new wiadomosc();
            secret_number = generator.nextInt(30) + 1;

            System.out.println("\nWylosowana liczba tajna: " + secret_number);

            do
            {
                buffer_in = new byte[2];
                buffer_out = new byte[2];
                packet = new byte[2];

                //CZEKANIE NA DWOCH KLIENTOW
                if(counter == 0 || counter == 1)
                {
                    //OTRZYMANIE PIERWSZEGO PAKIETU
                    inPacket = new DatagramPacket(buffer_in, packet_size);
                    datagramSocket.receive(inPacket);
                    clientAddress = inPacket.getAddress();
                    clientPort = inPacket.getPort();
                    counter++;

                    // PIERWSZY KLIENT
                    if(counter == 1)
                    {
                        System.out.println("\nPolaczono z adresem: " + clientAddress + "\nWygenerowane ID: " + id1);
                        clientAddress1 = inPacket.getAddress();
                        clientPort1 = inPacket.getPort();
                    }
                    //DRUGI KLIENT
                    else if(counter == 2)
                    {
                        System.out.println("\nPolaczono z adresem: " + clientAddress + " \nWygenerowane ID: " + id2);
                    }
                    unpack();

                    oper = txt.POWITANIE;
                    odp = txt.OK;
                    if(counter == 1)
                    {
                        id = id1;
                    }
                    else if(counter == 2)
                    {
                        id = id2;
                    }
                    liczba = txt.NUM;

                    //WYSLANIE PAKIETU Z ID
                    pack_in();
                    System.out.print("PAKIET: ");
                    display_packet();
                    outPacket = new DatagramPacket(buffer_out, packet_size, clientAddress, clientPort);
                    datagramSocket.send(outPacket);

                    //ODEBRANIE PAKIETU Z LICZBA L1 I L2
                    inPacket = new DatagramPacket(buffer_in, packet_size);
                    datagramSocket.receive(inPacket);
                    clientAddress = inPacket.getAddress();
                    clientPort = inPacket.getPort();
                    unpack();

                    attempts += bits_to_int(liczba);

                    //WYZNACZENIE LICZBY PROB I PRZESLANIE INFORMACJI DO OBU KLIENTOW
                    if(counter == 2)
                    {
                        buffer_in = new byte[2];
                        buffer_out = new byte[2];
                        attempts = attempts / 2;
                        System.out.println("\nLiczba prob zostala ustalona: " + attempts);

                        //DRUGI KLIENT
                        outPacket = new DatagramPacket(buffer_out, packet_size, clientAddress, clientPort);
                        liczba = Integer.toBinaryString(attempts);
                        liczba = df5.format(Integer.parseInt(liczba));
                        pack_in();
                        datagramSocket.send(outPacket);

                        //PIERWSZY KLIENT
                        outPacket = new DatagramPacket(buffer_out, packet_size, clientAddress1, clientPort1);
                        id = id1;
                        pack_in();
                        datagramSocket.send(outPacket);
                    }
                }
                //JESLI DWOCH KLIENTOW JEST JUZ POLACZONYCH
                else
                {
                    buffer_in = new byte[2];
                    buffer_out = new byte[2];
                    packet = new byte[2];

                    //ODEBRANIE ZGADYWANEJ LICZBY
                    inPacket = new DatagramPacket(buffer_in, packet_size);
                    datagramSocket.receive(inPacket);
                    clientAddress = inPacket.getAddress();
                    clientPort = inPacket.getPort();
                    unpack();

                    if(oper.equals("0000") && id.equals("000") && liczba.equals("00000") && odp.equals("000"))
                    {
                        outPacket = new DatagramPacket(buffer_out, packet_size, clientAddress, clientPort);
                        reset_packet();
                        id = txt.FULL;
                        pack_in();
                        datagramSocket.send(outPacket);
                    }

                    if(id.equals("0000"))
                    {
                        System.out.println("\nKTOS PROBOWAL SIE POLACZYC");
                    }
                    else
                    {
                        System.out.println("KLIENT o ID " + id + ": " + bits_to_int(liczba));
                    }


                    //ODSYLANIE KLIENTOM INFORMACJI O POPRAWNOSCI
                    outPacket = new DatagramPacket(buffer_out, buffer_out.length, clientAddress, clientPort);
                    if(bits_to_int(liczba) == secret_number)
                    {
                        if(if_end == false)
                        {
                            oper = txt.WYNIK;
                            odp = txt.WYGR;
                            pack_in();
                            datagramSocket.send(outPacket);
                            System.out.println("\nGra zakonczona. Wygral klient o ID: " + id);
                            if_end = true;
                        }
                        else
                        {
                            oper = txt.WYNIK;
                            odp = txt.PRZEGR;
                            pack_in();
                            datagramSocket.send(outPacket);
                            break;
                        }
                    }
                    else
                    {
                        oper = txt.WYNIK;
                        odp = txt.BLAD;
                        pack_in();
                        datagramSocket.send(outPacket);
                    }
                }
            } while(true);

        } catch(Exception e) {
            e.printStackTrace();
        } finally {
            System.out.println("\n Zamykanie polaczenia...");
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

    // STRING NA INT
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