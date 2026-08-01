import { capitalize, formatPrice } from '../utils/formatters';
import './OfferCard.css';

export default function OfferCard({ offer }) {
  return (
    <article className="offer-card">
      <div className="offer-card-header">
        <span className="offer-source">{capitalize(offer.source)}</span>
      </div>

      <div className="offer-card-body">
        <div className="offer-price">{formatPrice(offer.price)}</div>

        <a
          className="offer-link"
          href={offer.siteLink}
          target="_blank"
          rel="noopener noreferrer"
        >
          Visit Store
        </a>
      </div>
    </article>
  );
}

