package com.revapps.customerapi.controller;

import com.revapps.customerapi.dto.CreateCustomerRequest;
import com.revapps.customerapi.dto.CustomerResponse;
import com.revapps.customerapi.dto.UpdateCustomerRequest;
import com.revapps.customerapi.service.CustomerService;
import io.micrometer.core.annotation.Timed;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for customer management operations with Dapr integration
 * 
 * Provides complete CRUD API with proper HTTP status codes,
 * validation, pagination, and comprehensive logging.
 * 
 * All operations trigger events published to Azure Service Bus via Dapr.
 */
@RestController
@RequestMapping("/api/v1/customers")
@CrossOrigin(origins = "*") // Configure properly for production
public class CustomerController {

    private static final Logger logger = LoggerFactory.getLogger(CustomerController.class);

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    /**
     * Creates a new customer
     * 
     * @param request Customer creation request with name and email
     * @return Created customer with HTTP 201 status
     */
    @PostMapping
    @Timed(value = "customer.create", description = "Time taken to create customer")
    public ResponseEntity<CustomerResponse> createCustomer(@Valid @RequestBody CreateCustomerRequest request) {
        logger.info("POST /api/v1/customers - Creating customer with email: {} [Dapr Integration]", request.email());
        
        CustomerResponse customer = customerService.createCustomer(request);
        
        logger.info("Customer created successfully with id: {} [Dapr Integration]", customer.id());
        return ResponseEntity.status(HttpStatus.CREATED).body(customer);
    }

    /**
     * Retrieves a customer by ID
     * 
     * @param id Customer ID
     * @return Customer details with HTTP 200 status
     */
    @GetMapping("/{id}")
    @Timed(value = "customer.get", description = "Time taken to retrieve customer")
    public ResponseEntity<CustomerResponse> getCustomer(@PathVariable Integer id) {
        logger.info("GET /api/v1/customers/{} - Retrieving customer [Dapr Integration]", id);
        
        CustomerResponse customer = customerService.getCustomer(id);
        
        return ResponseEntity.ok(customer);
    }

    /**
     * Retrieves all customers with pagination and sorting
     * 
     * @param page Page number (0-based, default: 0)
     * @param size Page size (default: 20, max: 100)
     * @param sortBy Field to sort by (default: id)
     * @param sortDir Sort direction (asc/desc, default: asc)
     * @return Paginated list of customers with HTTP 200 status
     */
    @GetMapping
    @Timed(value = "customer.getAll", description = "Time taken to retrieve customers")
    public ResponseEntity<Page<CustomerResponse>> getAllCustomers(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "20") int size,
            @RequestParam(value = "sortBy", defaultValue = "id") String sortBy,
            @RequestParam(value = "sortDir", defaultValue = "asc") String sortDir) {
        
        logger.info("GET /api/v1/customers - Retrieving customers: page={}, size={}, sortBy={}, sortDir={} [Dapr Integration]", 
                   page, size, sortBy, sortDir);
        
        // Validate and limit page size
        if (size > 100) {
            size = 100;
            logger.warn("Page size limited to 100");
        }
        
        // Create sort direction
        Sort.Direction direction = sortDir.equalsIgnoreCase("desc") ? 
            Sort.Direction.DESC : Sort.Direction.ASC;
        
        // Create pageable
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        
        Page<CustomerResponse> customers = customerService.getAllCustomers(pageable);
        
        logger.info("Retrieved {} customers from page {} [Dapr Integration]", customers.getNumberOfElements(), page);
        return ResponseEntity.ok(customers);
    }

    /**
     * Searches customers by name
     * 
     * @param name Name search term (case-insensitive partial match)
     * @return List of matching customers with HTTP 200 status
     */
    @GetMapping("/search")
    @Timed(value = "customer.search", description = "Time taken to search customers")
    public ResponseEntity<List<CustomerResponse>> searchCustomers(
            @RequestParam("name") String name) {
        
        logger.info("GET /api/v1/customers/search - Searching customers by name: {} [Dapr Integration]", name);
        
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("Search name cannot be empty");
        }
        
        List<CustomerResponse> customers = customerService.searchCustomersByName(name.trim());
        
        logger.info("Found {} customers matching name: {} [Dapr Integration]", customers.size(), name);
        return ResponseEntity.ok(customers);
    }

    /**
     * Updates an existing customer
     * 
     * @param id Customer ID to update
     * @param request Customer update request with name and email
     * @return Updated customer with HTTP 200 status
     */
    @PutMapping("/{id}")
    @Timed(value = "customer.update", description = "Time taken to update customer")
    public ResponseEntity<CustomerResponse> updateCustomer(
            @PathVariable Integer id,
            @Valid @RequestBody UpdateCustomerRequest request) {
        
        logger.info("PUT /api/v1/customers/{} - Updating customer with email: {} [Dapr Integration]", id, request.email());
        
        CustomerResponse customer = customerService.updateCustomer(id, request);
        
        logger.info("Customer updated successfully with id: {} [Dapr Integration]", customer.id());
        return ResponseEntity.ok(customer);
    }

    /**
     * Deletes a customer
     * 
     * @param id Customer ID to delete
     * @return HTTP 204 No Content status
     */
    @DeleteMapping("/{id}")
    @Timed(value = "customer.delete", description = "Time taken to delete customer")
    public ResponseEntity<Void> deleteCustomer(@PathVariable Integer id) {
        logger.info("DELETE /api/v1/customers/{} - Deleting customer [Dapr Integration]", id);
        
        customerService.deleteCustomer(id);
        
        logger.info("Customer deleted successfully with id: {} [Dapr Integration]", id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Health check endpoint
     * 
     * @return Simple health status
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Reversible Customer API with Dapr is healthy");
    }
}
