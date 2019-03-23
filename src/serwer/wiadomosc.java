//@author Michal Najborowski & Maciej Anglart

package serwer;

public class wiadomosc {

    //POLE OPERACJI
    String POWITANIE = "0001"; // PRZESYLANY JEST PAKIET POWITALNY
    String LICZBA = "0010"; // PRZESYLANA JEST LICZBA/start
    String WYNIK = "0011"; // PRZESYLANY JEST WYNIK

    //POLE ODPOWIEDZI
    String WYGR = "011"; //3
    String PRZEGR = "100"; //4
    String BLAD = "101"; //5
    String OK = "110"; //6
    String ERROR = "111"; //7

    //POLE ID
    String FULL = "0000";

    //POLE NUM
    String NUM = "00000";
}