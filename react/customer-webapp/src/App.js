import React, { useState, useEffect } from 'react';
import axios from 'axios';
import './App.css';

function App() {
  // State
  const [customers, setCustomers] = useState([]);
  const [loading, setLoading] = useState(false);
  const [currentPage, setCurrentPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [showModal, setShowModal] = useState(false);
  const [editingCustomer, setEditingCustomer] = useState(null);
  const [customerForm, setCustomerForm] = useState({ name: '', email: '' });
  const [notification, setNotification] = useState({ show: false, message: '', type: 'success' });

  // API base:
  // - If REACT_APP_REVERSIBLE_API_URL is provided at build time (docker-compose), use it (e.g. http://localhost:8081)
  // - Otherwise, default to window.location.origin so the app can call relative /api/* (Helm injects Nginx proxy)
  const rawEnvUrl = process.env.REACT_APP_REVERSIBLE_API_URL;
  const defaultBase = (typeof window !== 'undefined' && window.location && window.location.origin) ? window.location.origin : '';
  const API_URL = (rawEnvUrl && rawEnvUrl.trim() !== '' ? rawEnvUrl : defaultBase).replace(/\/$/, '');

  // Initial fetch
  useEffect(() => { loadCustomers(); }, []);

  async function loadCustomers(page = 0) {
    setLoading(true);
    try {
      const base = API_URL || '';
      const url = `${base}/api/v1/customers?page=${page}&size=20`;
      const resp = await axios.get(url);
      if (resp.data && Array.isArray(resp.data.content)) {
        setCustomers(resp.data.content);
        setCurrentPage(resp.data.number || 0);
        setTotalPages(resp.data.totalPages || 1);
      } else if (Array.isArray(resp.data)) { // Fallback if backend returns plain list
        setCustomers(resp.data);
        setCurrentPage(0);
        setTotalPages(1);
      } else {
        setCustomers([]);
        setCurrentPage(0);
        setTotalPages(0);
      }
    } catch (e) {
      console.error('Error loading customers:', e);
      setCustomers([]);
      showNotification(`Error loading customers: ${e.message}`, 'error');
    } finally {
      setLoading(false);
    }
  }

  function openAddModal() {
    setEditingCustomer(null);
    setCustomerForm({ name: '', email: '' });
    setShowModal(true);
  }

  function openEditModal(customer) {
    setEditingCustomer(customer);
    setCustomerForm({ name: customer.name, email: customer.email });
    setShowModal(true);
  }

  function closeModal() {
    setShowModal(false);
    setEditingCustomer(null);
    setCustomerForm({ name: '', email: '' });
  }

  function handleFormChange(e) {
    const { name, value } = e.target;
    setCustomerForm(prev => ({ ...prev, [name]: value }));
  }

  async function handleFormSubmit(e) {
    e.preventDefault();
    if (!customerForm.name.trim() || !customerForm.email.trim()) {
      showNotification('Please fill in all required fields', 'warning');
      return;
    }

  const isEditing = !!editingCustomer;
  const base = API_URL || '';
  const url = isEditing ? `${base}/api/v1/customers/${editingCustomer.id}` : `${base}/api/v1/customers`;
    const method = isEditing ? 'put' : 'post';
    try {
      await axios[method](url, customerForm);
      closeModal();
      showNotification(`Customer ${isEditing ? 'updated' : 'created'} successfully!`, 'success');
      loadCustomers(currentPage);
    } catch (e) {
      console.error('Error saving customer:', e);
      showNotification(`Error ${isEditing ? 'updating' : 'creating'} customer: ${e.message}`, 'error');
    }
  }

  async function deleteCustomer(id) {
    if (!window.confirm('Are you sure you want to delete this customer?')) return;
    try {
      const base = API_URL || '';
      await axios.delete(`${base}/api/v1/customers/${id}`);
      showNotification('Customer deleted successfully!', 'success');
      loadCustomers(currentPage);
    } catch (e) {
      console.error('Error deleting customer:', e);
      showNotification(`Error deleting customer: ${e.message}`, 'error');
    }
  }

  function changePage(page) {
    if (page >= 0 && page < totalPages) {
      loadCustomers(page);
    }
  }

  function showNotification(message, type = 'success') {
    setNotification({ show: true, message, type });
    setTimeout(() => setNotification({ show: false, message: '', type: 'success' }), 5000);
  }

  function formatDate(dateString) {
    return dateString ? new Date(dateString).toLocaleDateString() : 'N/A';
  }

  return (
    <div className="App">
      <div className="header">
        <h1>Customer Management</h1>
        <p>Manage your customers with ease</p>
      </div>
      <div className="container">
        <div className="controls">
          <div className="controls-right">
            <button className="btn btn-primary" onClick={openAddModal}>➕ Add Customer</button>
            <button className="btn btn-secondary" onClick={() => loadCustomers(currentPage)}>🔄 Refresh</button>
          </div>
        </div>
        <div className="main-content">
          {loading ? (
            <div className="loading">Loading customers...</div>
          ) : customers.length === 0 ? (
            <div className="empty-state">
              <h3>No customers found</h3>
              <p>Start by adding your first customer to get started.</p>
              <button className="btn btn-primary" onClick={openAddModal}>Add Customer</button>
            </div>
          ) : (
            <>
              <div className="customer-grid">
                <div className="grid-header">ID</div>
                <div className="grid-header">Name</div>
                <div className="grid-header">Email</div>
                <div className="grid-header">Created</div>
                <div className="grid-header">Updated</div>
                <div className="grid-header">Actions</div>
                {customers.map(c => (
                  <div key={c.id} className="customer-row">
                    <div className="customer-id">#{c.id}</div>
                    <div className="customer-name">{c.name || 'N/A'}</div>
                    <div className="customer-email">{c.email || 'N/A'}</div>
                    <div className="timestamp">{formatDate(c.createdAt)}</div>
                    <div className="timestamp">{formatDate(c.updatedAt)}</div>
                    <div className="action-buttons">
                      <button className="btn btn-primary btn-small" onClick={() => openEditModal(c)}>✏️ Edit</button>
                      <button className="btn btn-danger btn-small" onClick={() => deleteCustomer(c.id)}>🗑️ Delete</button>
                    </div>
                  </div>
                ))}
              </div>
              {totalPages > 1 && (
                <div className="pagination">
                  <button className="btn btn-secondary" disabled={currentPage === 0} onClick={() => changePage(currentPage - 1)}>← Previous</button>
                  <span className="page-info">Page {currentPage + 1} of {totalPages}</span>
                  <button className="btn btn-secondary" disabled={currentPage === totalPages - 1} onClick={() => changePage(currentPage + 1)}>Next →</button>
                </div>
              )}
            </>
          )}
        </div>

        {showModal && (
          <div className="modal show" onClick={closeModal}>
            <div className="modal-content" onClick={e => e.stopPropagation()}>
              <div className="modal-header">
                <h2 className="modal-title">{editingCustomer ? 'Edit Customer' : 'Add Customer'}</h2>
              </div>
              <form onSubmit={handleFormSubmit} className="modal-form">
                <div className="modal-body">
                  <div className="form-group">
                    <label htmlFor="customerName">Name *</label>
                    <input id="customerName" name="name" type="text" className="form-control" value={customerForm.name} onChange={handleFormChange} required />
                  </div>
                  <div className="form-group">
                    <label htmlFor="customerEmail">Email *</label>
                    <input id="customerEmail" name="email" type="email" className="form-control" value={customerForm.email} onChange={handleFormChange} required />
                  </div>
                </div>
                <div className="modal-footer">
                  <button type="button" className="btn btn-secondary" onClick={closeModal}>Cancel</button>
                  <button type="submit" className="btn btn-primary">{editingCustomer ? 'Update Customer' : 'Add Customer'}</button>
                </div>
              </form>
            </div>
          </div>
        )}

        {notification.show && (
          <div className={`notification ${notification.type}`}>
            <span>{notification.message}</span>
          </div>
        )}
      </div>
    </div>
  );
}

export default App;
