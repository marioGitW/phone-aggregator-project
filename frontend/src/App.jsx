import HomePage from './pages/HomePage'
import ProductPage from './pages/ProductPage'
import {BrowserRouter, Route, Routes} from 'react-router-dom'
import PhoneAnalyticsPage from './pages/PhoneAnalyticsPage';

import './App.css'

function App() {
    return (
        <BrowserRouter>
            <main>
                <Routes>
                    <Route path="/" element={<HomePage/>}/>
                    <Route path="/product/:id" element={<ProductPage/>}/>
                    <Route path="/analytics"  element={<PhoneAnalyticsPage/>}/>
                </Routes>
            </main>
        </BrowserRouter>
    )
}

export default App
