import { BrowserRouter, Route, Routes } from 'react-router-dom';
import { createTheme, ThemeProvider } from '@mui/material/styles';
import { HomePage } from './pages';

import './index.css';
import {AppBar, Typography} from "@mui/material";

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
            <AppBar position='static'>
                <Typography variant='h6' component='div' sx={{ flexGrow: 1 }} style={{ padding: '8px 16px' }}>Disaster Adviser</Typography>
            </AppBar>
            <main style={{ padding: '8px 16px' }}>
                <BrowserRouter>
                    <Routes>
                        { routes }
                    </Routes>
                </BrowserRouter>
            </main>
        </ThemeProvider>
    );
};

export default App;