package com.Learning.app;

import com.Learning.model.Car;
import com.Learning.model.Mazda;
import com.Learning.model.Vehicle;

import java.util.ArrayList;
import java.util.List;

public class Program {
    // T = Type\ Tipo de Clase
    // K = Key \ Llave
    // V = Value \ Valor
    // E = Element \ Elemento
    // N = Number \ Numero
    // Z  = Otro \ elemento
    // ? = Wildcard \ Comodin sin restricciones \ unkown type \ ? extends Objetc \ ?

    // UPPER-BOUNDED
    // algo tiene que ser menor o igual que "Car" se refiere a nivel jerarquia es decir como ejemplo herencia
    //  public void m1(List<? extends Car> list) {
    public  void m1(List<? extends Car> list) {

    }
    // lower-bounded
    // algo tiene que ser mayor igual que "Car"
    public  void m2(List<? super Car> list) {

    }

    public static void main(String[] args) {
        Program app = new Program();
        List<Vehicle> list = new ArrayList<>();
        app.m2(list);

    }
}
