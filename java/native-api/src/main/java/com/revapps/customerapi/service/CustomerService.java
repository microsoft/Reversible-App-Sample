package com.revapps.customerapi.service;

import com.revapps.customerapi.dto.CreateCustomerRequest;
import com.revapps.customerapi.dto.CustomerResponse;
import com.revapps.customerapi.dto.UpdateCustomerRequest;
import com.revapps.customerapi.entity.Customer;
import com.revapps.customerapi.exception.CustomerNotFoundException;
import com.revapps.customerapi.exception.DuplicateEmailException;
import com.revapps.customerapi.repository.CustomerRepository;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service layer for customer operations with event publishing
 * 
 * Features:
 * - Complete CRUD operations with validation
 * - Event publishing to Azure Service Bus for all operations
 * - Transactional support for data consistency
 * - Comprehensive error handling and logging
 * - Email uniqueness validation
 */
@Service
@Transactional
public class CustomerService {

    private static final Logger logger = LoggerFactory.getLogger(CustomerService.class);
    
    private final CustomerRepository customerRepository;
    private final CustomerEventPublisher eventPublisher;
    private final Tracer tracer;

    public CustomerService(CustomerRepository customerRepository, 
                          CustomerEventPublisher eventPublisher,
                          Tracer tracer) {
        this.customerRepository = customerRepository;
        this.eventPublisher = eventPublisher;
        this.tracer = tracer;
    }

    /**
     * Creates a new customer and publishes a creation event
     */
    public CustomerResponse createCustomer(CreateCustomerRequest request) {
        Span span = tracer.spanBuilder("customer.create")
                .setAttribute("customer.email", request.email())
                .setAttribute("customer.name", request.name())
                .setAttribute("db.operation", "INSERT")
                .setAttribute("db.table", "customers")
                .startSpan();
        
        try (Scope scope = span.makeCurrent()) {
            logger.info("Creating customer with email: {}", request.email());

            // Check for duplicate email
            span.addEvent("database.duplicate_check.start");
            if (customerRepository.existsByEmailIgnoreCase(request.email())) {
                span.addEvent("duplicate_email_detected");
                logger.warn("Attempt to create customer with duplicate email: {}", request.email());
                throw new DuplicateEmailException("Customer with email '" + request.email() + "' already exists");
            }
            span.addEvent("database.duplicate_check.complete");

            // Create and save customer
            Customer customer = new Customer(request.name(), request.email());
            span.addEvent("database.insert.start");
            Customer savedCustomer = customerRepository.save(customer);
            span.addEvent("database.insert.complete");
            
            span.setAttribute("customer.id", savedCustomer.getId().toString());
            span.setAttribute("customer.created_at", savedCustomer.getCreatedAt().toString());

            logger.info("Customer created successfully: id={}, email={}", savedCustomer.getId(), savedCustomer.getEmail());

            // Publish creation event
            try {
                eventPublisher.publishCustomerCreated(
                    savedCustomer.getId(), 
                    savedCustomer.getName(), 
                    savedCustomer.getEmail()
                );
                span.addEvent("event_published");
            } catch (Exception e) {
                span.recordException(e);
                span.addEvent("event_publish_failed");
                logger.error("Failed to publish customer creation event for id={}, continuing anyway", savedCustomer.getId(), e);
                // Don't fail the operation if event publishing fails
            }

            return mapToResponse(savedCustomer);
        } catch (Exception e) {
            span.recordException(e);
            throw e;
        } finally {
            span.end();
        }
    }

    /**
     * Retrieves a customer by ID
     */
    @Transactional(readOnly = true)
    public CustomerResponse getCustomer(Integer id) {
        Span span = tracer.spanBuilder("customer.get")
                .setAttribute("customer.id", id.toString())
                .setAttribute("db.operation", "SELECT")
                .setAttribute("db.table", "customers")
                .startSpan();
        
        try (Scope scope = span.makeCurrent()) {
            logger.debug("Retrieving customer with id: {}", id);
            
            span.addEvent("database.query.start");
            Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> {
                    span.addEvent("customer.not_found");
                    return new CustomerNotFoundException("Customer with id " + id + " not found");
                });
            
            span.addEvent("database.query.complete");
            span.setAttribute("customer.found", true);
            span.setAttribute("customer.email", customer.getEmail());
            span.setAttribute("customer.name", customer.getName());
            
            return mapToResponse(customer);
        } catch (Exception e) {
            span.recordException(e);
            span.setAttribute("customer.found", false);
            throw e;
        } finally {
            span.end();
        }
    }

    /**
     * Retrieves all customers with pagination
     */
    @Transactional(readOnly = true)
    public Page<CustomerResponse> getAllCustomers(Pageable pageable) {
        Span span = tracer.spanBuilder("customer.get_all")
                .setAttribute("db.operation", "SELECT")
                .setAttribute("db.table", "customers")
                .setAttribute("pagination.page", pageable.getPageNumber())
                .setAttribute("pagination.size", pageable.getPageSize())
                .startSpan();
        
        try (Scope scope = span.makeCurrent()) {
            logger.debug("Retrieving customers with pagination: {}", pageable);
            
            span.addEvent("database.paginated_query.start");
            Page<Customer> customers = customerRepository.findAll(pageable);
            span.addEvent("database.paginated_query.complete");
            
            span.setAttribute("result.total_elements", customers.getTotalElements());
            span.setAttribute("result.total_pages", customers.getTotalPages());
            span.setAttribute("result.current_page", customers.getNumber());
            span.setAttribute("result.elements_in_page", customers.getNumberOfElements());
            
            return customers.map(this::mapToResponse);
        } catch (Exception e) {
            span.recordException(e);
            throw e;
        } finally {
            span.end();
        }
    }

    /**
     * Searches customers by name
     */
    @Transactional(readOnly = true)
    public List<CustomerResponse> searchCustomersByName(String name) {
        Span span = tracer.spanBuilder("customer.search")
                .setAttribute("db.operation", "SELECT")
                .setAttribute("db.table", "customers")
                .setAttribute("search.query", name)
                .startSpan();
        
        try (Scope scope = span.makeCurrent()) {
            logger.debug("Searching customers by name: {}", name);
            
            span.addEvent("database.search_query.start");
            List<Customer> customers = customerRepository.findByNameContainingIgnoreCase(name);
            span.addEvent("database.search_query.complete");
            
            span.setAttribute("search.results_count", customers.size());
            
            return customers.stream()
                .map(this::mapToResponse)
                .toList();
        } catch (Exception e) {
            span.recordException(e);
            throw e;
        } finally {
            span.end();
        }
    }

    /**
     * Updates an existing customer and publishes an update event
     */
    public CustomerResponse updateCustomer(Integer id, UpdateCustomerRequest request) {
        Span span = tracer.spanBuilder("customer.update")
                .setAttribute("customer.id", id.toString())
                .setAttribute("customer.new_email", request.email())
                .setAttribute("customer.new_name", request.name())
                .setAttribute("db.operation", "UPDATE")
                .setAttribute("db.table", "customers")
                .startSpan();
        
        try (Scope scope = span.makeCurrent()) {
            logger.info("Updating customer with id: {}", id);

            // Find existing customer
            span.addEvent("database.select.start");
            Customer existingCustomer = customerRepository.findById(id)
                .orElseThrow(() -> {
                    span.addEvent("customer.not_found");
                    return new CustomerNotFoundException("Customer with id " + id + " not found");
                });
            span.addEvent("database.select.complete");
            
            span.setAttribute("customer.old_email", existingCustomer.getEmail());
            span.setAttribute("customer.old_name", existingCustomer.getName());

            // Check for duplicate email (excluding current customer)
            span.addEvent("database.duplicate_check.start");
            if (customerRepository.existsByEmailIgnoreCaseAndIdNot(request.email(), id)) {
                span.addEvent("duplicate_email_detected");
                logger.warn("Attempt to update customer {} with duplicate email: {}", id, request.email());
                throw new DuplicateEmailException("Customer with email '" + request.email() + "' already exists");
            }
            span.addEvent("database.duplicate_check.complete");

            // Update customer fields
            existingCustomer.setName(request.name());
            existingCustomer.setEmail(request.email());
            
            span.addEvent("database.update.start");
            Customer updatedCustomer = customerRepository.save(existingCustomer);
            span.addEvent("database.update.complete");

            logger.info("Customer updated successfully: id={}, email={}", updatedCustomer.getId(), updatedCustomer.getEmail());

            // Publish update event
            try {
                eventPublisher.publishCustomerUpdated(
                    updatedCustomer.getId(), 
                    updatedCustomer.getName(), 
                    updatedCustomer.getEmail()
                );
                span.addEvent("event_published");
            } catch (Exception e) {
                span.recordException(e);
                span.addEvent("event_publish_failed");
                logger.error("Failed to publish customer update event for id={}, continuing anyway", updatedCustomer.getId(), e);
                // Don't fail the operation if event publishing fails
            }

            return mapToResponse(updatedCustomer);
        } catch (Exception e) {
            span.recordException(e);
            throw e;
        } finally {
            span.end();
        }
    }

    /**
     * Deletes a customer and publishes a deletion event
     */
    public void deleteCustomer(Integer id) {
        Span span = tracer.spanBuilder("customer.delete")
                .setAttribute("customer.id", id.toString())
                .setAttribute("db.operation", "DELETE")
                .setAttribute("db.table", "customers")
                .startSpan();
        
        try (Scope scope = span.makeCurrent()) {
            logger.info("Deleting customer with id: {}", id);

            // Find existing customer
            span.addEvent("database.select.start");
            Customer existingCustomer = customerRepository.findById(id)
                .orElseThrow(() -> {
                    span.addEvent("customer.not_found");
                    return new CustomerNotFoundException("Customer with id " + id + " not found");
                });
            span.addEvent("database.select.complete");

            // Store customer info for event before deletion
            String customerName = existingCustomer.getName();
            span.setAttribute("customer.name", customerName);
            span.setAttribute("customer.email", existingCustomer.getEmail());
            
            // Delete customer
            span.addEvent("database.delete.start");
            customerRepository.delete(existingCustomer);
            span.addEvent("database.delete.complete");

            logger.info("Customer deleted successfully: id={}, name={}", id, customerName);

            // Publish deletion event
            try {
                eventPublisher.publishCustomerDeleted(id, customerName);
                span.addEvent("event_published");
            } catch (Exception e) {
                span.recordException(e);
                span.addEvent("event_publish_failed");
                logger.error("Failed to publish customer deletion event for id={}, continuing anyway", id, e);
                // Don't fail the operation if event publishing fails
            }
        } catch (Exception e) {
            span.recordException(e);
            throw e;
        } finally {
            span.end();
        }
    }

    /**
     * Maps Customer entity to CustomerResponse DTO
     */
    private CustomerResponse mapToResponse(Customer customer) {
        return new CustomerResponse(
            customer.getId(),
            customer.getName(),
            customer.getEmail(),
            customer.getCreatedAt(),
            customer.getUpdatedAt()
        );
    }
}
