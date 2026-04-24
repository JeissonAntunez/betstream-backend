package com.Learning.controller;

import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "http://localhost:3000")
public class FormController {
    @PostMapping("/enviar")
    public String recibirDatos(@RequestBody Map<String,String> datos){
        System.out.println("Datos recibidos " + datos.get("nombre"));
        return  "Datos guardados correctamente";
    }

    @GetMapping("/productos")
    public List<Map<String, Object>> listarProductos() {
        // Esto es solo un ejemplo para que Astro reciba algo
        return List.of(
                Map.of("id", 1, "nombre", "Laptop"),
                Map.of("id", 2, "nombre", "Mouse")
        );
    }

}
