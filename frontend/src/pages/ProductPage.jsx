import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import OfferCard from '../components/OfferCard';
import { fetchProductOffers } from '../api/phoneService';
import './ProductPage.css';

export default function ProductPage() {
  const { id } = useParams();

  const [offers, setOffers] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(null);

  useEffect(() => {
    const loadOffers = async () => {
      setLoading(true);
      setError(null);

      try {
        const data = await fetchProductOffers(id);
        setOffers(data || []);
      } catch (err) {
        setError(err.message || 'Failed to load product offers.');
      } finally {
        setLoading(false);
      }
    };

    if (id) {
      loadOffers();
    }
  }, [id]);

  const productTitle = offers[0]?.title || 'Product offers';

  if (loading) {
    return (
      <div className="product-page">
        <p>Loading offers...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="product-page">
        <Link to="/" className="back-link">Back to phones</Link>
        <p className="error">Error: {error}</p>
      </div>
    );
  }

  return (
    <div className="product-page">
      <Link to="/" className="back-link">Back to phones</Link>
      <h1>{productTitle}</h1>
      <p className="offers-count">{offers.length} offer(s) found</p>

      {offers.length === 0 ? (
        <p>No matching offers found for this phone.</p>
      ) : (
        <div className="offers-list">
          {offers.map((offer) => (
            <OfferCard key={offer.id} offer={offer} />
          ))}
        </div>
      )}
    </div>
  );
}

