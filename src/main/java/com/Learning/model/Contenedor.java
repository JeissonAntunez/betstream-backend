package com.Learning.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


public class Contenedor<T> {

    private T contrasenia;

    public void guardar(T contrasenia){
        this.contrasenia = contrasenia;
    }

    public T obtener(){
        return  contrasenia;
    }

    public boolean estaVacio(){
        return  contrasenia == null;
    }


}
