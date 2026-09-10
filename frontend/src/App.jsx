import HomePage from './pages/HomePage'
import ProductPage from './pages/ProductPage'
import {BrowserRouter, Route, Routes} from 'react-router-dom'
import PhoneAnalyticsPage from './pages/PhoneAnalyticsPage';
import DemoBadge from './components/DemoBadge';
import DemoModeNotice from './components/DemoModeNotice';
import { DATA_MODE } from './api/dataClient';

import './App.css'

function App() {
    const isDemoMode = DATA_MODE === 'static';

    return (
        <BrowserRouter>
            <main>
                {isDemoMode && <DemoBadge/>}
                <Routes>
                    <Route path="/" element={<HomePage/>}/>
                    <Route path="/product/:id" element={<ProductPage/>}/>
                    <Route path="/analytics"  element={<PhoneAnalyticsPage/>}/>
                </Routes>
                {isDemoMode && <DemoModeNotice/>}
            </main>
        </BrowserRouter>
    )
}

export default App
