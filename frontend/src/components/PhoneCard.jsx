/**
 * PhoneCard
 * Reusable component to display a single phone
 * Presentational component - receives data as props, no state or side effects
 */

import { formatPrice, capitalize } from '../utils/formatters';
import { useNavigate } from 'react-router-dom';
import './PhoneCard.css';

export default function PhoneCard({ phone }) {
  const navigate = useNavigate();

  const {
    id,
    brand,
    title,
    price,
    source,
    siteLink,
    // Optional fields (not displayed in this milestone):
    // rawTitle,
    // createdAt,
    // imageUrl (will be added later)
  } = phone;

  const openProductPage = () => {
    navigate(`/product/${id}`);
  };

  const handleCardKeyDown = (event) => {
    if (event.key === 'Enter' || event.key === ' ') {
      event.preventDefault();
      openProductPage();
    }
  };

  return (
    <div
      className="phone-card"
      role="button"
      tabIndex={0}
      onClick={openProductPage}
      onKeyDown={handleCardKeyDown}
    >
      <div className="phone-card-header">
        <span className="phone-brand">{capitalize(brand)}</span>
        <span className="phone-source">{capitalize(source)}</span>
      </div>

      <div className="phone-card-body">
        <h3 className="phone-title">{title}</h3>

        <div className="phone-price">
          <strong>{formatPrice(price)}</strong>
        </div>

        <a
          href={siteLink}
          target="_blank"
          rel="noopener noreferrer"
          className="phone-link"
          onClick={(event) => event.stopPropagation()}
        >
          Visit Store
        </a>
      </div>
    </div>
  );
}

