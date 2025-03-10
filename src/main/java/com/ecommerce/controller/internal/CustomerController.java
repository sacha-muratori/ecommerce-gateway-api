package com.ecommerce.controller.internal;

import com.ecommerce.controller.internal.base.AbstractInternalController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/internal/customer")
public class CustomerController extends AbstractInternalController<List<String>> {

        private static final Map<String, List<String>> MOCK_CUSTOMERS = new HashMap<>() {
            {
                put("a3660234050", List.of("Mark", "Fabbri", "Seychelles"));
                put("b377889999", List.of("Hugo", "Erikson", "Luxembourg"));
                put("c322229999", List.of("Elisabeth", "Cierra", "Mexico"));
                put("d453217001", List.of("Alice", "Johnson", "Canada"));
                put("e987654321", List.of("Robert", "Smith", "USA"));
                put("f654987321", List.of("Emma", "Brown", "UK"));
                put("g321456987", List.of("Lucas", "Miller", "Germany"));
                put("h741852963", List.of("Olivia", "Garcia", "Spain"));
                put("i852369741", List.of("Liam", "Martinez", "Brazil"));
                put("j159753468", List.of("Sophia", "Lopez", "Argentina"));
                put("k753159852", List.of("Benjamin", "Gonzalez", "Chile"));
                put("l456789123", List.of("Charlotte", "Rodriguez", "Colombia"));
                put("m369258147", List.of("Ethan", "Fernandez", "Peru"));
                put("n147852369", List.of("Amelia", "Torres", "Venezuela"));
                put("o258147369", List.of("James", "Nguyen", "Vietnam"));
                put("p987321654", List.of("Mia", "Kim", "South Korea"));
                put("q741963852", List.of("Alexander", "Chen", "China"));
                put("r369147258", List.of("Isabella", "Singh", "India"));
                put("s852741963", List.of("Daniel", "Kumar", "Pakistan"));
                put("t123456789", List.of("Ava", "Takahashi", "Japan"));
            }
        };

        @Override
        protected Map<String, List<String>> getMockData () {
        return MOCK_CUSTOMERS;
    }
}
