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
                <Routes>
                    <Route path="/" element={<HomePage/>}/>
                    <Route path="/product/:id" element={<ProductPage/>}/>
                    <Route path="/analytics"  element={<PhoneAnalyticsPage/>}/>
                </Routes>
                {isDemoMode && (
                    <div className="fixed bottom-4 right-4 z-50 flex flex-col items-end gap-3">
                        <DemoModeNotice/>
                        <DemoBadge/>
                    </div>
                )}
            </main>
        </BrowserRouter>
    )
}

export default App
