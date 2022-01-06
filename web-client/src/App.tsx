import { BrowserRouter, Route, Routes } from 'react-router-dom';
import { createTheme, ThemeProvider } from '@material-ui/core/styles';
import { HomePage } from './pages';

import './index.css';

const theme = createTheme({
    palette: {
        primary: {
            main: '#202125',
        },
    },
});

const App = () => {
    const routes = [
        <Route key='/' path='/' element={<HomePage />} />
    ];

    return (
        <ThemeProvider theme={theme}>
            <BrowserRouter>
                <Routes>
                    { routes }
                </Routes>
            </BrowserRouter>
        </ThemeProvider>
    );
};

export default App;