import { describe, expect, test } from 'vitest';
import { render, screen } from '@testing-library/react';
import IndexPage from 'konfigyr/app/page';

describe('app/index', () => {
    render(<IndexPage />);

    test('should render welcome message', () => {
        const headline = screen.getByRole('heading', { level: 1 });
        expect(headline).toBeDefined()
        expect(headline).toHaveTextContent('Welcome to konfigyr.vault');
    });
});
