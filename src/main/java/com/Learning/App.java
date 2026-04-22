package com.Learning;

import com.Learning.model.Contenedor;
import com.Learning.model.Employee;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class App {
    public static <T> void intercambiar(T[] arreglo, int indice1, int indice2) {

        T temporal = arreglo[indice1];

        // Tu lógica aquí:
        // 1. Guarda el valor de la posición 1 en una variable temporal tipo T
        // 2. Pon el valor de la posición 2 en la posición 1
        // 3. Pon la variable temporal en la posición 2
    }
    public static void main(String[] args) {

        // Prueba con un array de Strings
        String[] nombres = {"Jeisson", "Juan", "Pedro"};
        intercambiar(nombres, 0, 2);

        // Imprime el array para ver si Pedro ahora está en la posición 0
        for(String n : nombres) System.out.println(n);

        // Reto extra: Haz lo mismo pero con un array de tus objetos 'Employee'
        /*List<Employee> lista = new ArrayList<>();

        Employee e1 = new Employee(1,"Jiesosn",2300);
        Employee e2 = new Employee(2,"Juaanb",1200);

        lista.add(e1);
        lista.add(e2);


        List<Employee> list4 = new ArrayList<>();

        list4.add(new Employee(1,"Jeisson",2500));
        list4.add(new Employee(2,"Perez",2500));
        list4.add(new Employee(3,"Flex",2500));

        Collections.shuffle(list4);

        System.out.println(list4);


        list4.forEach(e -> System.out.println(e));*/
        System.out.println("Hola");

        Contenedor<String> cajaTexto = new Contenedor<>();
        System.out.println("Caja esta vacia: " + cajaTexto.estaVacio());

        cajaTexto.guardar("Java2026@");
        System.out.println("Mi nueva contrasenia es : " + cajaTexto.obtener());

        Contenedor<Employee> cajaEmpleado = new Contenedor<>();
        Employee emp = new Employee(10,"Carlos",2000);

        cajaEmpleado.guardar(emp);
        System.out.println(cajaEmpleado.obtener());
        System.out.println(cajaEmpleado.obtener().getName());
        System.out.println(cajaEmpleado.obtener().getSalary());
    }



}
