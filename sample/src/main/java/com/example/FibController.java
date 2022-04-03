package com.example;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FibController {
    static int fib(int n) {
        // Base Case
        if (n <= 1)
            return n;

        // Recursive call
        return fib(n - 1)
                + fib(n - 2);
    }

    @GetMapping("/fib")
    public int fibHandler(@RequestParam(value = "n", defaultValue = "13") int n) {
        return fib(n);
    }
}
