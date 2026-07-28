/**
 * HomePage
 * Fetches phones from the backend and manages page state
 * Initial version: logs response to console for verification
 */

import { useState, useEffect } from 'react';
import { fetchPhones } from '../api/phoneService';
import PhoneCard from '../components/PhoneCard';
import './HomePage.css';

export default function HomePage() {
  const [phones, setPhones] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(20);
  const [totalPages, setTotalPages] = useState(0);

  useEffect(() => {
    const loadPhones = async () => {
      setLoading(true);
      setError(null);

      try {
        const response = await fetchPhones(page, pageSize);

        console.log('Full response from backend:', response);
        console.log('Phones received:', response.content);

        setPhones(response.content || []);
        setTotalPages(response.totalPages || 0);
      } catch (err) {
        console.error('Failed to load phones:', err);
        setError(err.message || 'Failed to load phones. Please try again.');
      } finally {
        setLoading(false);
      }
    };

    loadPhones();
  }, [page, pageSize]);

  const nextPage = () => {
    if (page < totalPages - 1) {
      setPage(page + 1);
    }
  };

  const previousPage = () => {
    if (page > 0) {
      setPage(page - 1);
    }
  };

  if (loading) {
    return <div className="home-page"><p>Loading phones...</p></div>;
  }

  if (error) {
    return (
      <div className="home-page">
        <p className="error">Error: {error}</p>
        <button onClick={() => window.location.reload()}>Retry</button>
      </div>
    );
  }

  return (
    <div className="home-page">
      <h1>Available Phones</h1>
      <p className="info">Found {phones.length} phones (Page {page + 1} of {totalPages})</p>

      {phones.length === 0 ? (
        <p>No phones found.</p>
      ) : (
        <>
          <div className="phones-list">
            {phones.map((phone) => (
              <PhoneCard key={phone.id} phone={phone} />
            ))}
          </div>

          <div className="pagination">
            <button
              onClick={previousPage}
              disabled={page === 0}
              className="pagination-btn"
            >
              ← Previous
            </button>

            <span className="pagination-info">
              Page {page + 1} of {totalPages}
            </span>

            <button
              onClick={nextPage}
              disabled={page === totalPages - 1}
              className="pagination-btn"
            >
              Next →
            </button>
          </div>
        </>
      )}
    </div>
  );
}

